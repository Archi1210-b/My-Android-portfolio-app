package com.example.portfolioapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth

// Activity 2: Login / Register / Forgot password (Firebase Authentication)
class LoginActivity : BaseActivity() {
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            if (!isValid()) return@setOnClickListener
            auth.signInWithEmailAndPassword(text(etEmail), text(etPassword))
                .addOnSuccessListener { goHome() }
                .addOnFailureListener { Utils.toast(this, it.message) }
        }

        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            if (!isValid()) return@setOnClickListener
            auth.createUserWithEmailAndPassword(text(etEmail), text(etPassword))
                .addOnSuccessListener {
                    Utils.toast(this, "Account created! Complete your profile to get started.")
                    goHome()
                }
                .addOnFailureListener { Utils.toast(this, it.message) }
        }

        findViewById<Button>(R.id.btnForgot).setOnClickListener {
            val email = text(etEmail)
            if (email.isEmpty()) {
                Utils.toast(this, "Type your email first, then tap Forgot password")
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener { Utils.toast(this, "Password reset link sent to $email") }
                    .addOnFailureListener { Utils.toast(this, it.message) }
            }
        }
    }

    private fun isValid(): Boolean {
        if (text(etEmail).isEmpty() || text(etPassword).length < 6) {
            Utils.toast(this, "Enter your email and a password of at least 6 characters")
            return false
        }
        return true
    }

    private fun text(e: EditText) = e.text.toString().trim()

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}