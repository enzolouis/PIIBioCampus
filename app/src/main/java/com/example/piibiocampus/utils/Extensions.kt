package com.example.piibiocampus.utils

import android.content.Context
import android.widget.Toast

object Extensions {
    fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }
}