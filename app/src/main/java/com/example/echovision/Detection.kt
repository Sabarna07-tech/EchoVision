package com.example.echovision

import android.graphics.RectF

// This is a simple data class to hold the information for a single detected object.
data class Detection(
    val boundingBox: RectF, // The bounding box of the detected object.
    val label: String,      // The label of the object (e.g., "person", "car").
    val confidence: Float   // The confidence score of the detection.
)
