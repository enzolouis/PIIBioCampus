package com.example.piibiocampus.utils

import android.app.Activity
import android.support.annotation.StringRes
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.piibiocampus.R

/**
 * extension if this is an Activity
 * call "setTopBarTitle("...") at any location of your activity
 */
fun Activity.setTopBarTitle(text: String) {
    findViewById<TextView>(R.id.topBarTitle)?.text = text
}

fun Activity.setTopBarTitle(@StringRes resId: Int) {
    findViewById<TextView>(R.id.topBarTitle)?.setText(resId)
}

/**
 * extension if this is a Fragment
 * call "setTopBarTitle("...") at any location of your fragment
 */
fun Fragment.setTopBarTitle(text: String) {
    requireActivity().setTopBarTitle(text)
}

fun Fragment.setTopBarTitle(@StringRes resId: Int) {
    requireActivity().setTopBarTitle(resId)
}