package com.example.piibiocampus.ui.photo

import android.graphics.*
import com.squareup.picasso.Transformation
import androidx.core.graphics.createBitmap

/**
 * Transformation Picasso qui découpe l'image en cercle.
 * Utilisé pour afficher la photo de profil de l'auteur en rond.
 */
class CircleTransform : Transformation {

    override fun transform(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val x = (source.width  - size) / 2
        val y = (source.height - size) / 2

        val squared = Bitmap.createBitmap(source, x, y, size, size)
        if (squared != source) source.recycle()

        val result = createBitmap(size, size, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        paint.shader = shader
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)
        squared.recycle()

        return result
    }

    override fun key(): String = "circle"
}
