package com.example.portfolioapp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

// Saves small PDFs (resume / project reports) inside Firestore as Base64 text:
//   users/{uid}/files/{key}   -> name, size, data
// (Firebase Storage needs a paid plan, so this works on the free plan. Limit: 700 KB.)
object PdfStore {
    const val MAX_BYTES = 700 * 1024

    private fun ref(key: String) = FirebaseFirestore.getInstance()
        .collection("users").document(FirebaseAuth.getInstance().currentUser!!.uid)
        .collection("files").document(key)

    fun fileName(context: Context, uri: Uri): String {
        var name = "document.pdf"
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0 && c.moveToFirst()) name = c.getString(i)
        }
        return name
    }

    // onResult(null) = success, otherwise an error message
    fun save(context: Context, key: String, uri: Uri, onResult: (String?) -> Unit) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            if (bytes.size > MAX_BYTES) {
                onResult("PDF is bigger than 700 KB. Upload it to Google Drive and paste the link instead.")
                return
            }
            val data = hashMapOf<String, Any>(
                "name" to fileName(context, uri),
                "size" to bytes.size,
                "data" to Base64.encodeToString(bytes, Base64.NO_WRAP)
            )
            ref(key).set(data)
                .addOnSuccessListener { onResult(null) }
                .addOnFailureListener { onResult(it.message) }
        } catch (e: Exception) {
            onResult(e.message)
        }
    }

    fun delete(key: String) {
        ref(key).delete()
    }

    private fun toCacheUri(context: Context, doc: DocumentSnapshot): Uri {
        val bytes = Base64.decode(doc.getString("data") ?: "", Base64.NO_WRAP)
        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val safe = (doc.getString("name") ?: "document.pdf").replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(dir, if (safe.endsWith(".pdf", true)) safe else "$safe.pdf")
        file.writeBytes(bytes)
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }

    fun open(context: Context, key: String) {
        ref(key).get().addOnSuccessListener { doc ->
            if (!doc.exists()) { Utils.toast(context, "No PDF found"); return@addOnSuccessListener }
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(toCacheUri(context, doc), "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Utils.toast(context, "No PDF viewer app installed on this device")
            }
        }.addOnFailureListener { Utils.toast(context, it.message) }
    }

    fun share(context: Context, key: String) {
        ref(key).get().addOnSuccessListener { doc ->
            if (!doc.exists()) { Utils.toast(context, "No PDF found"); return@addOnSuccessListener }
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, toCacheUri(context, doc))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Share PDF via"))
        }.addOnFailureListener { Utils.toast(context, it.message) }
    }
}