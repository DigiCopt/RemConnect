package com.inovace.remconnect

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
//hii
class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        emailEditText = findViewById(R.id.emailaddress)
        passwordEditText = findViewById(R.id.passwordinput)
        loginButton = findViewById(R.id.loginButton)
        firebaseAuth = FirebaseAuth.getInstance()

        // Check if the user is already logged in
        val currentUser: FirebaseUser? = firebaseAuth.currentUser
        if (currentUser != null) {
            // User is already authenticated, redirect to MainActivity
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish() // Close the LoginActivity
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (!email.isNullOrBlank() && !password.isNullOrBlank()) {
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(OnSuccessListener { authResult ->
                        // User login successful
                        // Save login state
                        saveLoginState(true)
                        // Redirect to MainActivity
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish() // Close the LoginActivity
                    })
                    .addOnFailureListener(OnFailureListener { e ->
                        // Handle login failure by showing an error dialog
                        showErrorDialog("Login Failed", "Invalid email or password. Please try again.")
                    })
            }
        }
    }

    // Save login state using SharedPreferences
    private fun saveLoginState(isLoggedIn: Boolean) {
        val sharedPreferences: SharedPreferences = getSharedPreferences("loginState", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", isLoggedIn)
        editor.apply()
    }

    // Check if the user is already logged in
    private fun isUserLoggedIn(): Boolean {
        val sharedPreferences: SharedPreferences = getSharedPreferences("loginState", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isLoggedIn", false)
    }

    // Show an error dialog
    private fun showErrorDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
            })
            .show()
    }
}
