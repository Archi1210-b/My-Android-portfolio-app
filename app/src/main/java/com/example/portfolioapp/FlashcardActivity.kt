package com.example.portfolioapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Activity 7: Each project shown as one compact card with all the key points
class FlashcardActivity : BaseActivity() {
    private var list: List<Project> = emptyList()
    private var index = 0
    private var ownerName = ""

    private lateinit var flashContent: View
    private lateinit var tvEmpty: View
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var btnGithub: Button
    private lateinit var btnDemo: Button
    private lateinit var btnPdf: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flashcard)
        setupToolbar("Project Flashcards")

        flashContent = findViewById(R.id.flashContent)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        btnGithub = findViewById(R.id.btnFGithub)
        btnDemo = findViewById(R.id.btnFDemo)
        btnPdf = findViewById(R.id.btnFPdf)

        btnPrev.setOnClickListener { if (index > 0) { index--; show() } }                   // BACK
        btnNext.setOnClickListener { if (index < list.size - 1) { index++; show() } }       // FORWARD
        findViewById<Button>(R.id.btnFShare).setOnClickListener {
            if (list.isNotEmpty()) CardImage.share(this, list[index])    // shares the card as a picture
        }
        btnGithub.setOnClickListener { Utils.openUrl(this, list[index].github) }
        btnDemo.setOnClickListener { Utils.openUrl(this, list[index].demoLink) }
        btnPdf.setOnClickListener {
            val p = list[index]
            if (p.hasPdf) PdfStore.open(this, "project_${p.id}") else Utils.openUrl(this, p.pdfLink)
        }

        load()
    }

    private fun load() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        userRef.get().addOnSuccessListener { u ->
            ownerName = u.getString("name") ?: ""
            userRef.collection("projects").get().addOnSuccessListener { snap ->
                list = snap.documents.mapNotNull { Project.from(it) }.sortedBy { it.createdAt }
                val startId = intent.getStringExtra("projectId")
                index = list.indexOfFirst { it.id == startId }.coerceAtLeast(0)
                show()
            }
        }
    }

    private fun show() {
        if (list.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            flashContent.visibility = View.GONE
            return
        }
        tvEmpty.visibility = View.GONE
        flashContent.visibility = View.VISIBLE
        val p = list[index]

        findViewById<TextView>(R.id.tvCounter).text = "Project ${index + 1} of ${list.size}"
        ProjectCardBinder.bind(findViewById(R.id.projectCard), p, ownerName)
        findViewById<ScrollView>(R.id.cardScroll).scrollTo(0, 0)

        btnPrev.isEnabled = index > 0
        btnNext.isEnabled = index < list.size - 1
        btnGithub.visibility = if (p.github.isBlank()) View.GONE else View.VISIBLE
        btnDemo.visibility = if (p.demoLink.isBlank()) View.GONE else View.VISIBLE
        btnPdf.visibility = if (p.hasPdf || p.pdfLink.isNotBlank()) View.VISIBLE else View.GONE
    }
}