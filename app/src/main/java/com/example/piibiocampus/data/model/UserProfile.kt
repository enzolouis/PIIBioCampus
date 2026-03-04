package com.example.piibiocampus.data.model

data class UserProfile(
    var uid: String = "",
    val name: String = "",
    val email: String = "",
    val description: String = "",
    val profilePictureUrl: String = "",
    val role: String = ""
)