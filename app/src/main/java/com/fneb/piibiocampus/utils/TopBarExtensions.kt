package com.fneb.piibiocampus.utils

import android.app.Activity
import android.support.annotation.StringRes
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.fneb.piibiocampus.R

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

fun Activity.showTopBarLeftButton(onClick: () -> Unit) {
    findViewById<ImageButton>(R.id.topBarLeftButton)?.let { btn ->
        btn.visibility = View.VISIBLE
        btn.setOnClickListener { onClick() }
    }
}

fun Activity.hideTopBarLeftButton() {
    findViewById<ImageButton>(R.id.topBarLeftButton)?.visibility = View.GONE
}

fun Fragment.showTopBarLeftButton(onClick: () -> Unit) {
    requireActivity().showTopBarLeftButton(onClick)
}

fun Fragment.hideTopBarLeftButton() {
    requireActivity().hideTopBarLeftButton()
}