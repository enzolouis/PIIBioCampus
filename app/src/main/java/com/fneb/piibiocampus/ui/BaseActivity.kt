package com.fneb.piibiocampus.ui
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fneb.piibiocampus.R
import com.fneb.piibiocampus.network.NetworkMonitor
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt


abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var networkMonitor: NetworkMonitor
    private var floatingNetworkIcon: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        networkMonitor = NetworkMonitor.getInstance(applicationContext)
        observeNetwork()
    }

    private fun observeNetwork() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.isOnline.collect { online ->
                    android.util.Log.d("NETWORK", "isOnline = $online")
                    updateNetworkIcon(!online)
                }
            }
        }
    }

    private fun updateNetworkIcon(show: Boolean) {
        if (show) {
            if (floatingNetworkIcon == null) {
                floatingNetworkIcon = ImageView(this).apply {
                    setImageResource(R.drawable.ic_signal_wifi_off)
                    imageTintList = ColorStateList.valueOf("#E53935".toColorInt())
                    val sizePx = (64 * resources.displayMetrics.density).toInt()
                    layoutParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                        gravity = Gravity.CENTER
                    }
                }
                window.decorView.let {
                    (it as? FrameLayout)?.addView(floatingNetworkIcon)
                        ?: (it as ViewGroup).addView(floatingNetworkIcon)
                }
            }
        } else {
            floatingNetworkIcon?.let {
                (it.parent as? ViewGroup)?.removeView(it)
            }
            floatingNetworkIcon = null
        }
    }
}