package com.example.portfolioapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

// Activity 4: Profile - photo, personal details, bio, skills, links and resume (saved at users/{uid})
class ProfileActivity : BaseActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser!!
    private val fields = LinkedHashMap<String, EditText>()   // firestore key -> input box

    private lateinit var imgPhoto: ImageView
    private lateinit var tvInitials: TextView
    private lateinit var tvResumeStatus: TextView
    private lateinit var btnViewResume: Button
    private lateinit var btnShareResume: Button

    // file pickers (PDF for resume, image for profile photo)
    private val pickResume = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uploadResume(uri)
    }
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uploadPhoto(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setupToolbar("My Profile")

        val ids = mapOf(
            "name" to R.id.etName, "headline" to R.id.etHeadline, "college" to R.id.etCollege,
            "branch" to R.id.etBranch, "gradYear" to R.id.etGradYear, "cgpa" to R.id.etCgpa,
            "phone" to R.id.etPhone, "location" to R.id.etLocation, "bio" to R.id.etBio,
            "skills" to R.id.etSkills, "github" to R.id.etGithub, "linkedin" to R.id.etLinkedin,
            "resumeLink" to R.id.etResumeLink
        )
        for ((key, id) in ids) fields[key] = findViewById<EditText>(id)

        imgPhoto = findViewById(R.id.imgPhoto)
        tvInitials = findViewById(R.id.tvInitials)
        tvResumeStatus = findViewById(R.id.tvResumeStatus)
        btnViewResume = findViewById(R.id.btnViewResume)
        btnShareResume = findViewById(R.id.btnShareResume)

        // Load saved data so the student can edit it
        db.collection("users").document(user.uid).get().addOnSuccessListener { d ->
            for ((key, box) in fields) box.setText(d.getString(key) ?: "")
            tvInitials.text = Utils.initials(d.getString("name"))
            Utils.showAvatar(imgPhoto, d.getString("photo"))
            if (d.getBoolean("hasResume") == true) showResume(d.getString("resumeName") ?: "resume.pdf")
        }

        findViewById<Button>(R.id.btnChangePhoto).setOnClickListener { pickPhoto.launch("image/*") }
        findViewById<Button>(R.id.btnRemovePhoto).setOnClickListener { removePhoto() }
        findViewById<Button>(R.id.btnUploadResume).setOnClickListener { pickResume.launch("application/pdf") }
        btnViewResume.setOnClickListener { PdfStore.open(this, "resume") }
        btnShareResume.setOnClickListener { PdfStore.share(this, "resume") }
        findViewById<Button>(R.id.btnSave).setOnClickListener { save(false) }
        findViewById<Button>(R.id.btnSaveNext).setOnClickListener { save(true) }
    }

    // ---------- profile photo ----------
    private fun uploadPhoto(uri: Uri) {
        val text = ImageUtils.uriToBase64(this, uri)
        if (text == null) {
            Utils.toast(this, "Could not read this image")
            return
        }
        db.collection("users").document(user.uid).set(mapOf("photo" to text), SetOptions.merge())
            .addOnSuccessListener {
                Utils.showAvatar(imgPhoto, text)
                Utils.toast(this, "Profile photo updated")
            }
            .addOnFailureListener { Utils.toast(this, it.message) }
    }

    private fun removePhoto() {
        db.collection("users").document(user.uid)
            .set(mapOf<String, Any>("photo" to FieldValue.delete()), SetOptions.merge())
            .addOnSuccessListener {
                imgPhoto.visibility = View.GONE
                Utils.toast(this, "Photo removed")
            }
            .addOnFailureListener { Utils.toast(this, it.message) }
    }

    // ---------- resume ----------
    private fun showResume(name: String) {
        tvResumeStatus.text = "✔ $name"
        btnViewResume.visibility = View.VISIBLE
        btnShareResume.visibility = View.VISIBLE
    }

    private fun uploadResume(uri: Uri) {
        val name = PdfStore.fileName(this, uri)
        PdfStore.save(this, "resume", uri) { error ->
            if (error != null) {
                Utils.toast(this, error)
            } else {
                db.collection("users").document(user.uid)
                    .set(mapOf("hasResume" to true, "resumeName" to name), SetOptions.merge())
                showResume(name)
                Utils.toast(this, "Resume uploaded")
            }
        }
    }

    // ---------- save text fields ----------
    private fun save(openNext: Boolean) {
        val nameBox = fields["name"]!!
        if (nameBox.text.toString().trim().isEmpty()) {
            nameBox.error = "Name is required"
            return
        }
        val data = HashMap<String, Any>()
        for ((key, box) in fields) data[key] = box.text.toString().trim()
        data["email"] = user.email ?: ""

        db.collection("users").document(user.uid).set(data, SetOptions.merge())
            .addOnSuccessListener {
                Utils.toast(this, "Profile saved ✔")
                if (openNext) startActivity(Intent(this, AddProjectActivity::class.java))   // FORWARD
                finish()
            }
            .addOnFailureListener { Utils.toast(this, it.message) }
    }
}