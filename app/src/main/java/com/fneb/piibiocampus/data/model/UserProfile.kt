package com.fneb.piibiocampus.data.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val description: String = "",
    val profilePictureUrl: String = "",
    var role: String = "",
    val currentBadge: String = ""
)