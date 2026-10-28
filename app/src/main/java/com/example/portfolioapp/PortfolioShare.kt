package com.example.portfolioapp

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.DocumentSnapshot
import java.io.File

// "Share portfolio" menu: PDF (recommended), view PDF, or plain text
object PortfolioShare {

    fun show(activity: Activity, profile: DocumentSnapshot?, projects: List<Project>) {
        val options = arrayOf("Share as PDF (recommended)", "View PDF", "Share as plain text")
        MaterialAlertDialogBuilder(activity)
            .setTitle("Share portfolio")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> makePdf(activity, profile, projects) { file ->
                        Utils.shareFile(activity, file, "application/pdf", "Portfolio", caption(profile))
                    }
                    1 -> makePdf(activity, profile, projects) { file ->
                        Utils.viewFile(activity, file, "application/pdf")
                    }
                    else -> Utils.shareText(activity, "My Portfolio", Utils.portfolioText(profile, projects))
                }
            }
            .show()
    }

    private fun makePdf(
        activity: Activity,
        profile: DocumentSnapshot?,
        projects: List<Project>,
        onReady: (File) -> Unit
    ) {
        Utils.toast(activity, "Creating your portfolio PDF...")
        PortfolioPdf.create(activity, profile, projects) { file, error ->
            if (file != null) onReady(file) else Utils.toast(activity, "Could not create PDF: $error")
        }
    }

    private fun caption(profile: DocumentSnapshot?): String {
        val name = profile?.getString("name") ?: ""
        val sb = StringBuilder("Hello! Please find my portfolio attached")
        if (name.isNotBlank()) sb.append(" - ").append(name)
        sb.append(".\nTap the project names and links inside the PDF to explore my work.")
        val github = profile?.getString("github") ?: ""
        val linkedin = profile?.getString("linkedin") ?: ""
        if (github.isNotBlank()) sb.append("\nGitHub: ").append(github)
        if (linkedin.isNotBlank()) sb.append("\nLinkedIn: ").append(linkedin)
        return sb.toString()
    }
}