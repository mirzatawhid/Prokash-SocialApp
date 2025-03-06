package com.dcoder.prokash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.dcoder.prokash.databinding.ActivityProfileBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage


class ProfileActivity : AppCompatActivity() {

    private lateinit var binding:ActivityProfileBinding
    private lateinit var auth:FirebaseAuth
    private lateinit var db:FirebaseFirestore
    private lateinit var storageRef:FirebaseStorage
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        db = Firebase.firestore
        storageRef = Firebase.storage

        if (userId!=null){
            db.collection("user").document(userId).get().addOnSuccessListener {
                if(it.exists()){
                    binding.profileName.text = it["fullname"].toString()
                    binding.profileUsername.text = "@"+it["username"].toString()
                }
            }.addOnFailureListener{
                Log.d("ProfileSection", "Profile Data retrieve error : $it")
            }

            // using glide library to display the image
            storageRef.reference.child("user").child(userId.toString()).child("profile_photo").downloadUrl.addOnSuccessListener {
                if (it.toString().isNotEmpty()){
                    Glide.with(this)
                        .load(it)
                        .into(binding.profileImage)
                }
            }.addOnFailureListener {
                Log.e("Firebase", "No Profile Picture yet")
            }

        }

        binding.btnEditProfile.setOnClickListener{
            startActivity(Intent(this@ProfileActivity,EditProfileActivity::class.java))
        }



    }
}