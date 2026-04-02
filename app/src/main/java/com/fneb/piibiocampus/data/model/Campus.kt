package com.fneb.piibiocampus.data.model

data class PolygonPoint(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

data class Campus(
    val name: String = "",
    val latitudeCenter: Double = 0.0,
    val longitudeCenter: Double = 0.0,
    val radius: Double = 0.0,
    val polygon: List<PolygonPoint> = emptyList(),
    val id: String,
)