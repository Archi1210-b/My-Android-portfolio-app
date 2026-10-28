package com.example.portfolioapp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.firestore.DocumentSnapshot
import java.io.File

object Utils {

    fun toast(context: Context, msg: String?) =
        Toast.makeText(context, msg ?: "Something went wrong", Toast.LENGTH_LONG).show()

    // Opens GitHub / LinkedIn / any link in the browser
    fun openUrl(context: Context, url: String?) {
        if (url.isNullOrBlank()) { toast(context, "No link added yet"); return }
        val full = if (url.startsWith("http")) url else "https://$url"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(full)))
        } catch (e: Exception) {
            toast(context, "Cannot open this link")
        }
    }

    // Opens the Android share sheet with plain text
    fun shareText(context: Context, subject: String, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, "Share via"))
    }

    fun fileUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

    // Shares a file (PDF or image) together with a text caption
    fun shareFile(context: Context, file: File, mime: String, subject: String, text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, fileUri(context, file))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share via"))
    }

    // Opens a file in another app (e.g. a PDF viewer)
    fun viewFile(context: Context, file: File, mime: String) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(fileUri(context, file), mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(context, "No PDF viewer app installed on this device")
        }
    }

    // Shows the profile photo on top of the initials circle (or hides it if there is none)
    fun showAvatar(photoView: ImageView, base64: String?) {
        val bmp = ImageUtils.base64ToBitmap(base64)
        if (bmp != null) {
            photoView.setImageBitmap(bmp)
            photoView.visibility = View.VISIBLE
        } else {
            photoView.visibility = View.GONE
        }
    }

    fun initials(name: String?): String {
        val parts = (name ?: "").trim().split(" ").filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
        }
    }

    private fun StringBuilder.addLine(label: String, value: String) {
        if (value.isNotBlank()) append(label).append(value).append("\n")
    }

    // Text version of one project (caption when sharing a flashcard)
    fun projectText(p: Project): String {
        val sb = StringBuilder()
        sb.append("🚀 ").append(p.title).append("\n")
        sb.addLine("", p.tagline)
        sb.append("\n")
        sb.addLine("Problem: ", p.problem)
        sb.addLine("Solution: ", p.solution)
        sb.addLine("My role: ", p.role)
        sb.addLine("Outcome: ", p.outcome)
        sb.addLine("Tech: ", p.techStack)
        sb.addLine("GitHub: ", p.github)
        sb.addLine("Demo: ", p.demoLink)
        sb.addLine("Report: ", p.pdfLink)
        return sb.toString().trim()
    }

    // Text version of the whole portfolio
    fun portfolioText(profile: DocumentSnapshot?, projects: List<Project>): String {
        fun g(k: String) = profile?.getString(k) ?: ""
        val sb = StringBuilder()
        sb.append("👤 ").append(g("name")).append("\n")
        sb.addLine("", g("headline"))
        sb.addLine("🎓 ", listOf(g("college"), g("branch")).filter { it.isNotBlank() }.joinToString(", "))
        sb.addLine("CGPA: ", g("cgpa"))
        sb.append("\n")
        sb.addLine("About: ", g("bio"))
        sb.addLine("Skills: ", g("skills"))
        sb.addLine("GitHub: ", g("github"))
        sb.addLine("LinkedIn: ", g("linkedin"))
        sb.addLine("Resume: ", g("resumeLink"))
        sb.addLine("Email: ", g("email"))
        sb.addLine("Phone: ", g("phone"))
        if (projects.isNotEmpty()) {
            sb.append("\n📁 Projects (").append(projects.size).append("):\n")
            for (p in projects) {
                sb.append("• ").append(p.title)
                if (p.tagline.isNotBlank()) sb.append(" - ").append(p.tagline)
                sb.append("\n")
                if (p.techStack.isNotBlank()) sb.append("   Tech: ").append(p.techStack).append("\n")
                if (p.github.isNotBlank()) sb.append("   ").append(p.github).append("\n")
            }
        }
        return sb.toString().trim()
    }
}