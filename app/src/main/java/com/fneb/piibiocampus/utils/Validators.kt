package com.fneb.piibiocampus.utils

object Validators {

    fun areEmailAndPasswordValid (
        email: String,
        password: String
    ): Boolean {

        return email.isNotEmpty() || password.isNotEmpty()
    }
}