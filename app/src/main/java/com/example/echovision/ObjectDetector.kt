package com.example.echovision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class ObjectDetector(
    private val context: Context,
    private val detectorListener: DetectorListener?
) {
    private var interpreter: Interpreter
    private val labels = mutableListOf<String>()

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0

    init {
        val model = loadModelFile(context)
        interpreter = Interpreter(model)
        initModelShape()
        loadLabels()
    }

    private fun loadModelFile(context: Context): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd("yolov5m-fp16.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels() {
        context.assets.open("labels.txt").bufferedReader().forEachLine {
            labels.add(it)
        }
    }

    private fun initModelShape() {
        val inputTensor = interpreter.getInputTensor(0)
        val inputShape = inputTensor.shape()
        tensorWidth = inputShape[1]
        tensorHeight = inputShape[2]

        val outputTensor = interpreter.getOutputTensor(0)
        val outputShape = outputTensor.shape()
        numChannel = outputShape[1]
        numElements = outputShape[2]
    }

    fun detect(image: Bitmap) {
        val resizedBitmap = Bitmap.createScaledBitmap(image, tensorWidth, tensorHeight, false)
        val byteBuffer = bitmapToByteBuffer(resizedBitmap)
        val output = Array(1) { Array(numElements) { FloatArray(numChannel) } }
        interpreter.run(byteBuffer, output)
        val detections = processOutput(output[0])
        detectorListener?.onResults(detections)
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * tensorWidth * tensorHeight * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(tensorWidth * tensorHeight)
        bitmap.getPixels(intValues, 0, tensorWidth, 0, 0, tensorWidth, tensorHeight)

        var pixel = 0
        for (i in 0 until tensorWidth) {
            for (j in 0 until tensorHeight) {
                val `val` = intValues[pixel++]
                byteBuffer.putFloat(((`val` shr 16) and 0xFF) / 255.0f)
                byteBuffer.putFloat(((`val` shr 8) and 0xFF) / 255.0f)
                byteBuffer.putFloat((`val` and 0xFF) / 255.0f)
            }
        }
        return byteBuffer
    }

    private fun processOutput(output: Array<FloatArray>): List<Detection> {
        val detections = mutableListOf<Detection>()
        val confidenceThreshold = 0.4f

        for (i in 0 until numElements) {
            var maxConf = -1.0f
            var maxIdx = -1
            var j = 4
            while (j < numChannel) {
                if (output[i][j] > maxConf) {
                    maxConf = output[i][j]
                    maxIdx = j - 4
                }
                j++
            }

            if (maxConf > confidenceThreshold) {
                val clsName = labels[maxIdx]
                val cx = output[i][0]
                val cy = output[i][1]
                val w = output[i][2]
                val h = output[i][3]
                val x1 = cx - w / 2
                val y1 = cy - h / 2
                val x2 = cx + w / 2
                val y2 = cy + h / 2
                if (x1 < 0f || x1 > 1f || y1 < 0f || y1 > 1f || x2 < 0f || x2 > 1f || y2 < 0f || y2 > 1f) continue

                detections.add(
                    Detection(
                        boundingBox = RectF(x1, y1, x2, y2),
                        label = clsName,
                        confidence = maxConf
                    )
                )
            }
        }
        return detections
    }

    interface DetectorListener {
        fun onResults(results: List<Detection>)
    }
}
