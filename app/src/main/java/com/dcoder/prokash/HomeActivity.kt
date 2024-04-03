package com.dcoder.prokash

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dcoder.prokash.databinding.ActivityHomeBinding
import com.dcoder.prokash.databinding.ActivityLoginBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class HomeActivity : AppCompatActivity() {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var binding: ActivityHomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        this.mAuth = Firebase.auth

        binding.loginButton.setOnClickListener{
            startActivity(Intent(this,LoginActivity::class.java))
        }

        binding.signupButton.setOnClickListener{
            startActivity(Intent(this,RegistrationActivity::class.java))
        }

    }

    override fun onStart() {
        super.onStart()
        val currentUser = mAuth.currentUser
        if(currentUser != null) run {
            val value = Intent(this, MainActivity::class.java)
            startActivity(value)
        }
    }
}