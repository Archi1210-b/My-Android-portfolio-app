package com.example.portfolioapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Activity 6: All projects with search, open flashcard, share, edit and delete
class ProjectListActivity : BaseActivity() {
    private val all = mutableListOf<Project>()
    private lateinit var adapter: ProjectAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText

    private val col by lazy {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("projects")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_list)
        setupToolbar("My Projects")

        tvEmpty = findViewById(R.id.tvEmpty)
        etSearch = findViewById(R.id.etSearch)

        adapter = ProjectAdapter(
            onOpen = { startActivity(Intent(this, FlashcardActivity::class.java).putExtra("projectId", it.id)) },
            onShare = { CardImage.share(this, it) },
            onEdit = { startActivity(Intent(this, AddProjectActivity::class.java).putExtra("projectId", it.id)) },
            onDelete = { confirmDelete(it) }
        )
        val rv = findViewById<RecyclerView>(R.id.recyclerView)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        etSearch.doAfterTextChanged { applyFilter() }
        findViewById<ExtendedFloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, AddProjectActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadProjects()
    }

    private fun loadProjects() {
        col.get().addOnSuccessListener { snap ->
            all.clear()
            all.addAll(snap.documents.mapNotNull { Project.from(it) }.sortedByDescending { it.createdAt })
            applyFilter()
        }
    }

    private fun applyFilter() {
        val q = etSearch.text.toString().trim().lowercase()
        val shown = if (q.isEmpty()) all else all.filter {
            listOf(it.title, it.tagline, it.techStack, it.category).any { f -> f.lowercase().contains(q) }
        }
        adapter.submit(shown)
        tvEmpty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmDelete(p: Project) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete project?")
            .setMessage("\"${p.title}\" will be removed permanently.")
            .setPositiveButton("Delete") { _, _ ->
                col.document(p.id).delete().addOnSuccessListener {
                    PdfStore.delete("project_${p.id}")
                    loadProjects()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}