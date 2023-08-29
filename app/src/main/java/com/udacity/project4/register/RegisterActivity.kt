package com.udacity.project4.register

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityRegisterBinding
import com.udacity.project4.login.LoginActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 120
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.registerButton.setOnClickListener {
            registerAccount()
        }

        binding.registerButtonGoogle.setOnClickListener {
            registerWithGoogleIntent()
        }

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun registerAccount() {
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()
        val intent = Intent(this, LoginActivity::class.java)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    this.startActivity(intent)
                } else {
                    Toast.makeText(this, "Error, please try again", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerWithGoogleIntent() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    createAccountWithGoogle(account)
                } else {
                    Toast.makeText(this, "Account is null", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed with error code: ${e.statusCode}", Toast.LENGTH_LONG).show()
                Log.e("RegisterActivity", "Google sign in failed", e)
            }
        }
    }


    private fun createAccountWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    if (task.result?.additionalUserInfo?.isNewUser == true) {
                        Toast.makeText(this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Conta Google vinculada com sucesso!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val message = task.exception?.message ?: "Unknown error"
                    Toast.makeText(this, "Falha na autenticação: $message", Toast.LENGTH_LONG).show()
                    Log.e("RegisterActivity", "Authentication failed", task.exception)
                }
            }
    }


}
