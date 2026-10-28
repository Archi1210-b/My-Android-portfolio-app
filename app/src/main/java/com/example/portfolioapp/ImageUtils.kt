package com.example.portfolioapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream

// Turns a gallery photo into a small square JPEG text (Base64) that is saved in Firestore.
object ImageUtils {
    private const val SIZE = 300

    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val square = loadSquare(context, uri) ?: return null
            val out = ByteArrayOutputStream()
            square.compress(Bitmap.CompressFormat.JPEG, 80, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadSquare(context: Context, uri: Uri): Bitmap? {
        val src: Bitmap? = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val big = maxOf(info.size.width, info.size.height)
                decoder.setTargetSampleSize((big / 1000).coerceAtLeast(1))
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 1000 && bounds.outHeight / sample > 1000) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }
        if (src == null) return null

        // crop to the centre square, then shrink to 300 x 300
        val side = minOf(src.width, src.height)
        val x = (src.width - side) / 2
        val y = (src.height - side) / 2
        val square = Bitmap.createBitmap(src, x, y, side, side)
        return Bitmap.createScaledBitmap(square, SIZE, SIZE, true)
    }

    fun base64ToBitmap(data: String?): Bitmap? {
        if (data.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(data, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}