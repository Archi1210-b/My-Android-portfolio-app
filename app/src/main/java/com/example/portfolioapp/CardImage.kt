package com.example.portfolioapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

// Turns a project card into a PNG picture and shares it (WhatsApp, LinkedIn, Gmail...)
object CardImage {

    fun render(context: Context, p: Project, owner: String): Bitmap {
        val view = LayoutInflater.from(context).inflate(R.layout.view_project_card, null, false)
        ProjectCardBinder.bind(view, p, owner)

        val density = context.resources.displayMetrics.density
        val width = (360 * density).toInt()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val pad = (16 * density).toInt()
        val scale = 2f   // 2x for a sharp image
        val bmp = Bitmap.createBitmap(
            ((view.measuredWidth + pad * 2) * scale).toInt(),
            ((view.measuredHeight + pad * 2) * scale).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        canvas.scale(scale, scale)
        canvas.drawColor(Color.parseColor("#F4F6FB"))
        canvas.translate(pad.toFloat(), pad.toFloat())
        view.draw(canvas)
        return bmp
    }

    fun share(context: Context, p: Project) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnCompleteListener { task ->
                val owner = if (task.isSuccessful) task.result?.getString("name") ?: "" else ""
                try {
                    val bmp = render(context, p, owner)
                    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                    val file = File(dir, "flashcard_${p.id}.png")
                    FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    // picture + a caption with the clickable links
                    Utils.shareFile(context, file, "image/png", p.title, Utils.projectText(p))
                } catch (e: Exception) {
                    Utils.toast(context, e.message)
                }
            }
    }
}