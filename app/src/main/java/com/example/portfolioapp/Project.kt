package com.example.portfolioapp

import com.google.firebase.firestore.DocumentSnapshot

data class Project(
    var id: String = "",
    var title: String = "",
    var tagline: String = "",
    var category: String = "Other",
    var description: String = "",
    var problem: String = "",
    var solution: String = "",
    var role: String = "",
    var outcome: String = "",
    var techStack: String = "",
    var github: String = "",
    var demoLink: String = "",
    var pdfLink: String = "",
    var hasPdf: Boolean = false,
    var createdAt: Long = 0
) {
    companion object {
        fun from(doc: DocumentSnapshot): Project? =
            doc.toObject(Project::class.java)?.also { it.id = doc.id }
    }
}