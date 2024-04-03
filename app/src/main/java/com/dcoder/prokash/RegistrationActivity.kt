package com.dcoder.prokash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.dcoder.prokash.databinding.ActivityRegistrationBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class RegistrationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)



        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.your_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this,gso)
        auth = Firebase.auth

        binding.btnGoogleReg.setOnClickListener{
            val signInClient = googleSignInClient.signInIntent
            launcher.launch(signInClient)
        }

        binding.button.setOnClickListener{
            val username = binding.usernameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val cpassword = binding.cpasswordEditText.text.toString()

            if(username.isEmpty()){
                Toast.makeText(this,"Please enter the username.",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(email.isEmpty()){
                Toast.makeText(this,"Please enter the email address.",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(password.isEmpty()){
                Toast.makeText(this,"Please enter the password.",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(password.length < 8){
                Toast.makeText(this,"Password too short, enter minimum 8 characters.",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if(password!=cpassword){
                Toast.makeText(this,"Password and Confirm Password Doesn't match.",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this){
                if (it.isSuccessful){
                    val db = Firebase.firestore
                    val userId = auth.currentUser?.uid
                    val user = hashMapOf(
                        "username" to username,
                        "email" to email
                    )

                    if (userId != null) {
                        db.collection("user").document(userId).set(user).addOnSuccessListener {
                            Toast.makeText(this,"Registration Successful!",Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this,MainActivity::class.java))
                            finish()
                        }.addOnFailureListener{
                            Toast.makeText(this,"Authentication Failed.",Toast.LENGTH_SHORT).show()
                        }
                    }

                }else{
                    Toast.makeText(this,"Authentication Failed.",Toast.LENGTH_SHORT).show()
                }
            }

        }

    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result->
        if(result.resultCode==Activity.RESULT_OK){

            val task=GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful){
                val account:GoogleSignInAccount?=task.result
                val credential=GoogleAuthProvider.getCredential(account?.idToken,null)
                auth.signInWithCredential(credential).addOnCompleteListener{
                    if(it.isSuccessful){
                        Toast.makeText(this,"Successful",Toast.LENGTH_LONG).show()
                        startActivity(Intent(this,MainActivity::class.java))
                    }else{
                        Toast.makeText(this,"Failed",Toast.LENGTH_LONG).show()
                    }
                }
            }

        }else{
            Toast.makeText(this,"Failed",Toast.LENGTH_LONG).show()
        }
    }

}