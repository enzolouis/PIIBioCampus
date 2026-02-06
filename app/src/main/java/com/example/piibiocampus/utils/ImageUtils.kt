package com.example.piibiocampus.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import java.io.ByteArrayOutputStream

object ImageUtils {

    fun resizeAndCompress(
        imageBytes: ByteArray,
        rotationDegrees: Int,
        maxWidth: Int = 1080,
        maxHeight: Int = 1920,
        quality: Int = 80
    ): ByteArray {

        // lire uniquement la taille
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

        // calcul du scale
        var scale = 1
        while (options.outWidth / scale > maxWidth || options.outHeight / scale > maxHeight) {
            scale *= 2
        }

        // décodage réel
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = scale
        }

        var bitmap = BitmapFactory.decodeByteArray(
            imageBytes,
            0,
            imageBytes.size,
            decodeOptions
        )

        // rotation
        if (rotationDegrees != 0) {
            val matrix = android.graphics.Matrix().apply {
                postRotate(rotationDegrees.toFloat())
            }

            bitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        }

        // format webp
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }

        // compression finale
        val baos = ByteArrayOutputStream()
        bitmap.compress(format, quality, baos)

        return baos.toByteArray()
    }

}
