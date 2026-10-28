package com.example.portfolioapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

// Activity 1: splash screen, then Login or Dashboard
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val loggedIn = FirebaseAuth.getInstance().currentUser != null
            val next = if (loggedIn) MainActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this, next))
            finish()
        }, 2000)
    }
}