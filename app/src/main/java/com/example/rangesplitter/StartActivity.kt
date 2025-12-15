package com.example.rangesplitter

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth   // 👈 IMPORTANT import

class StartActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val emailLayout = findViewById<TextInputLayout>(R.id.emailLayout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.passwordLayout)

        val strokeColor = ContextCompat.getColor(this, R.color.primaryAccent)

        emailLayout.setBoxStrokeColor(strokeColor)
        passwordLayout.setBoxStrokeColor(strokeColor)

        val hintColor = ContextCompat.getColor(this, R.color.textMuted) // or primaryAccent
        emailLayout.defaultHintTextColor = ColorStateList.valueOf(hintColor)
        passwordLayout.defaultHintTextColor  = ColorStateList.valueOf(hintColor)


        supportActionBar?.hide()
        window.navigationBarColor = resources.getColor(android.R.color.black)

        // FirebaseAuth instance (like in the examples)
        auth = FirebaseAuth.getInstance()

        val loginButton = findViewById<Button>(R.id.loginButton)
        val register = findViewById<TextView>(R.id.register)
        val username = findViewById<EditText>(R.id.emailInput)
        val password = findViewById<EditText>(R.id.passwordInput)

        loginButton.setOnClickListener {
            val email = username.text.toString().trim()
            val pass = password.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "Login failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
        register.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

    }
}
