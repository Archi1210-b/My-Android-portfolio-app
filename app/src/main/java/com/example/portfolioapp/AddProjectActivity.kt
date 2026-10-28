package com.example.portfolioapp

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

// Activity 5: Add a new project OR edit an existing one (saved at users/{uid}/projects/{id})
class AddProjectActivity : BaseActivity() {
    private val fields = LinkedHashMap<String, EditText>()
    private lateinit var actCategory: AutoCompleteTextView
    private lateinit var tvPdfStatus: TextView
    private lateinit var btnSave: Button

    private var projectId: String? = null       // not null when editing
    private var createdAt = System.currentTimeMillis()
    private var hasPdf = false
    private var pendingPdf: Uri? = null         // PDF chosen but not uploaded yet

    private val col by lazy {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("projects")
    }

    private val pickPdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingPdf = uri
            tvPdfStatus.text = "📄 ${PdfStore.fileName(this, uri)} (will upload when you save)"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_project)

        projectId = intent.getStringExtra("projectId")
        setupToolbar(if (projectId == null) "Add Project" else "Edit Project")

        val ids = mapOf(
            "title" to R.id.etTitle, "tagline" to R.id.etTagline, "description" to R.id.etDescription,
            "problem" to R.id.etProblem, "solution" to R.id.etSolution, "role" to R.id.etRole,
            "outcome" to R.id.etOutcome, "techStack" to R.id.etTech, "github" to R.id.etGithub,
            "demoLink" to R.id.etDemo, "pdfLink" to R.id.etPdfLink
        )
        for ((key, id) in ids) fields[key] = findViewById<EditText>(id)

        actCategory = findViewById(R.id.etCategory)
        val categories = listOf("Android App", "Web App", "AI / ML", "Data Science",
            "IoT / Embedded", "Game", "Research / Paper", "Other")
        actCategory.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, categories))

        tvPdfStatus = findViewById(R.id.tvPdfStatus)
        btnSave = findViewById(R.id.btnSaveProject)
        findViewById<Button>(R.id.btnPickPdf).setOnClickListener { pickPdf.launch("application/pdf") }
        btnSave.setOnClickListener { save() }

        projectId?.let { loadExisting(it) }
    }

    private fun loadExisting(id: String) {
        col.document(id).get().addOnSuccessListener { d ->
            for ((key, box) in fields) box.setText(d.getString(key) ?: "")
            actCategory.setText(d.getString("category") ?: "", false)
            createdAt = d.getLong("createdAt") ?: createdAt
            hasPdf = d.getBoolean("hasPdf") == true
            if (hasPdf) tvPdfStatus.text = "📄 A PDF is already attached (pick another to replace it)"
        }
    }

    private fun save() {
        val titleBox = fields["title"]!!
        if (titleBox.text.toString().trim().isEmpty()) {
            titleBox.error = "Project title is required"
            return
        }
        btnSave.isEnabled = false
        val id = projectId ?: col.document().id
        val upload = pendingPdf

        if (upload != null) {
            // upload the PDF first, then save the project
            PdfStore.save(this, "project_$id", upload) { error ->
                if (error != null) {
                    Utils.toast(this, error)
                    btnSave.isEnabled = true
                } else {
                    writeProject(id, true)
                }
            }
        } else {
            writeProject(id, hasPdf)
        }
    }

    private fun writeProject(id: String, pdf: Boolean) {
        val data = HashMap<String, Any>()
        for ((key, box) in fields) data[key] = box.text.toString().trim()
        data["category"] = actCategory.text.toString().trim().ifEmpty { "Other" }
        data["hasPdf"] = pdf
        data["createdAt"] = createdAt

        col.document(id).set(data, SetOptions.merge())
            .addOnSuccessListener {
                Utils.toast(this, "Project saved ✔")
                finish()
            }
            .addOnFailureListener {
                Utils.toast(this, it.message)
                btnSave.isEnabled = true
            }
    }
}