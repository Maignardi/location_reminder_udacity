package com.udacity.project4.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityLoginBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.register.RegisterActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.createAccountButton.setOnClickListener {
            clickedOnCreateAccount()
        }
        binding.loginButton.setOnClickListener {
            if(binding.emailEditText.isVisible){
                val email = binding.emailEditText.text.toString()
                val password = binding.passwordEditText.text.toString()
                loginUser(email, password)
            } else {
                binding.emailEditText.visibility = View.VISIBLE
                binding.passwordEditText.visibility = View.VISIBLE
            }
        }
        binding.googleLoginButton.setOnClickListener {
            loginWithGoogleAccount()
        }
    }

    fun clickedOnCreateAccount() {
        val intent = Intent(this, RegisterActivity::class.java)
        this.startActivity(intent)
    }

    fun loginWithGoogleAccount() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, RemindersActivity::class.java)
                    this.startActivity(intent)
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun loginUser(email: String, password: String) {
        val intent = Intent(this, RemindersActivity::class.java)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    this.startActivity(intent)
                } else {
                    Toast.makeText(this, "Error, please try again", Toast.LENGTH_SHORT).show()
                }
            }
    }

    companion object {
        const val RC_SIGN_IN = 9001
    }
}
