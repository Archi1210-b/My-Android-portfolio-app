package com.example.portfolioapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

// Activity 3: Dashboard - profile summary, profile strength, stats and navigation tiles
class MainActivity : BaseActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val uid: String get() = FirebaseAuth.getInstance().currentUser!!.uid
    private var profile: DocumentSnapshot? = null
    private var projects: List<Project> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupToolbar("My Portfolio", showBack = false)   // dashboard is the home screen

        open(R.id.tileProfile, ProfileActivity::class.java)
        open(R.id.tileAdd, AddProjectActivity::class.java)
        open(R.id.tileProjects, ProjectListActivity::class.java)
        open(R.id.tileFlash, FlashcardActivity::class.java)
        open(R.id.tilePreview, PortfolioActivity::class.java)
        findViewById<View>(R.id.tileShare).setOnClickListener {
            PortfolioShare.show(this, profile, projects)      // share as PDF
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()     // refresh every time we come back to the dashboard
    }

    private fun open(viewId: Int, target: Class<*>) {
        findViewById<View>(viewId).setOnClickListener { startActivity(Intent(this, target)) }
    }

    private fun loadData() {
        val userRef = db.collection("users").document(uid)
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

        findViewById<TextView>(R.id.tvName).text = s("name").ifBlank { "Student" }
        findViewById<TextView>(R.id.tvInitials).text = Utils.initials(s("name"))
        Utils.showAvatar(findViewById<ImageView>(R.id.imgPhoto), s("photo"))
        findViewById<TextView>(R.id.tvHeadline).text = s("headline").ifBlank { "Add your headline in My Profile" }
        findViewById<TextView>(R.id.tvCollege).text =
            listOf(s("college"), s("branch")).filter { it.isNotBlank() }.joinToString("  •  ")

        val skillCount = s("skills").split(",").count { it.isNotBlank() }
        val hasResume = d?.getBoolean("hasResume") == true || s("resumeLink").isNotBlank()

        findViewById<TextView>(R.id.tvStatProjects).text = projects.size.toString()
        findViewById<TextView>(R.id.tvStatSkills).text = skillCount.toString()
        findViewById<TextView>(R.id.tvStatResume).text = if (hasResume) "✔" else "✘"

        // Profile strength = how many of these 10 things are filled in
        val checks = listOf(
            "full name" to s("name").isNotBlank(),
            "headline" to s("headline").isNotBlank(),
            "college" to s("college").isNotBlank(),
            "branch" to s("branch").isNotBlank(),
            "bio" to s("bio").isNotBlank(),
            "skills" to (skillCount > 0),
            "GitHub link" to s("github").isNotBlank(),
            "LinkedIn link" to s("linkedin").isNotBlank(),
            "resume" to hasResume,
            "first project" to projects.isNotEmpty()
        )
        val percent = checks.count { it.second } * 100 / checks.size
        findViewById<TextView>(R.id.tvStrength).text = "Profile strength: $percent%"
        findViewById<LinearProgressIndicator>(R.id.progress).setProgressCompat(percent, true)

        val missing = checks.firstOrNull { !it.second }
        findViewById<TextView>(R.id.tvHint).text =
            if (missing == null) "🎉 Your portfolio is complete and recruiter-ready!"
            else "Next step: add your ${missing.first}"
    }
}