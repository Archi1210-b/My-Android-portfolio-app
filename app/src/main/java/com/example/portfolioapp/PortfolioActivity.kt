package com.example.portfolioapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

// Activity 8: Portfolio preview - how a recruiter would see the student's portfolio
class PortfolioActivity : BaseActivity() {
    private var profile: DocumentSnapshot? = null
    private var projects: List<Project> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_portfolio)
        setupToolbar("Portfolio Preview")

        findViewById<Button>(R.id.btnGithub).setOnClickListener {
            Utils.openUrl(this, profile?.getString("github"))
        }
        findViewById<Button>(R.id.btnLinkedin).setOnClickListener {
            Utils.openUrl(this, profile?.getString("linkedin"))
        }
        findViewById<Button>(R.id.btnResume).setOnClickListener { openResume() }
        findViewById<Button>(R.id.btnShare).setOnClickListener {
            PortfolioShare.show(this, profile, projects)      // PDF / view / text
        }
        load()
    }

    private fun openResume() {
        val p = profile
        if (p?.getBoolean("hasResume") == true) PdfStore.open(this, "resume")
        else Utils.openUrl(this, p?.getString("resumeLink"))
    }

    private fun load() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
        userRef.get().addOnSuccessListener { doc ->
            profile = doc
            userRef.collection("projects").get().addOnSuccessListener { snap ->
                projects = snap.documents.mapNotNull { Project.from(it) }.sortedBy { it.createdAt }
                bind()
            }
        }
    }

    private fun bind() {
        val d = profile
        fun s(key: String) = d?.getString(key) ?: ""

        findViewById<TextView>(R.id.tvName).text = s("name").ifBlank { "Your Name" }
        findViewById<TextView>(R.id.tvInitials).text = Utils.initials(s("name"))
        Utils.showAvatar(findViewById<ImageView>(R.id.imgPhoto), s("photo"))
        findViewById<TextView>(R.id.tvHeadline).text = s("headline")
        findViewById<TextView>(R.id.tvCollege).text =
            listOf(s("college"), s("branch")).filter { it.isNotBlank() }.joinToString("  •  ")
        findViewById<TextView>(R.id.tvAbout).text = s("bio").ifBlank { "No bio added yet." }

        val details = mutableListOf<String>()
        if (s("cgpa").isNotBlank()) details.add("🎓 CGPA: ${s("cgpa")}")
        if (s("gradYear").isNotBlank()) details.add("📅 Graduating: ${s("gradYear")}")
        if (s("location").isNotBlank()) details.add("📍 ${s("location")}")
        if (s("phone").isNotBlank()) details.add("📞 ${s("phone")}")
        if (s("email").isNotBlank()) details.add("✉ ${s("email")}")
        findViewById<TextView>(R.id.tvDetails).text = details.joinToString("\n")

        // skills as chips
        val group = findViewById<ChipGroup>(R.id.chipSkills)
        group.removeAllViews()
        val skills = s("skills").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (skills.isEmpty()) {
            group.addView(TextView(this).apply { text = "No skills added yet." })
        }
        for (skill in skills) {
            group.addView(Chip(this).apply { text = skill; isClickable = false })
        }

        // projects
        val container = findViewById<LinearLayout>(R.id.projectsContainer)
        container.removeAllViews()
        if (projects.isEmpty()) {
            container.addView(TextView(this).apply { text = "No projects added yet."; setPadding(8, 8, 8, 8) })
        }
        for (p in projects) {
            val card = LayoutInflater.from(this).inflate(R.layout.item_preview_project, container, false)
            card.findViewById<TextView>(R.id.pvTitle).text = p.title
            card.findViewById<TextView>(R.id.pvTagline).text = p.tagline
            card.findViewById<TextView>(R.id.pvTech).text = if (p.techStack.isBlank()) "" else "🛠 ${p.techStack}"
            card.setOnClickListener {
                startActivity(Intent(this, FlashcardActivity::class.java).putExtra("projectId", p.id))
            }
            container.addView(card)
        }
    }
}