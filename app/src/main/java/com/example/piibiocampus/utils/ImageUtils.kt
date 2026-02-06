package com.example.piibiocampus.utils
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun resizeAndCompress(
        imageBytes: ByteArray,
        maxWidth: Int = 1080,
        maxHeight: Int = 1920,
        quality: Int = 80
    ): ByteArray {
        // 1️⃣ Lire uniquement la taille de l'image
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

        // 2️⃣ Calculer le facteur d'échantillonnage
        var scale = 1
        while (options.outWidth / scale > maxWidth || options.outHeight / scale > maxHeight) {
            scale *= 2
        }

        // 3️⃣ Décoder le bitmap avec ce scale
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOptions)

        // 4️⃣ Compresser en WEBP pour réduire encore
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, baos)
        return baos.toByteArray()
    }
}