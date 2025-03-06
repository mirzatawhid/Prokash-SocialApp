package com.dcoder.prokash
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.bumptech.glide.Glide
import com.dcoder.prokash.databinding.ActivityEditProfileBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import java.text.SimpleDateFormat
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding:ActivityEditProfileBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth
    private lateinit var db:FirebaseFirestore
    private lateinit var storageRef:FirebaseStorage
    private lateinit var photoLauncher:ActivityResultLauncher<Intent>
    private var imageUri: Uri? = null

    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Camera permission granted
            pickImageFromGallery()
        } else {
            // Camera permission denied
            Toast.makeText(this,"Permission denied.",Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        db = Firebase.firestore
        storageRef = Firebase.storage

        photoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedImageUri: Uri? = result.data?.data
                selectedImageUri?.let { uri ->
                    // Persist URI permission
                    try {
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                    }
                    // Use Glide or another library to load the image from the URI
                    Glide.with(this)
                        .load(uri)
                        .into(binding.editImage)
                    // Set the global imageUri variable
                    imageUri = uri
                }
            }
        }


    //restoring the profile sata
        if (userId!=null){
            db.collection("user").document(userId).get().addOnSuccessListener {
                if(it.exists()){
                    binding.edittextFullname.setText(it["fullname"].toString())
                    binding.edittextUsername.setText(it["username"].toString())
                    binding.edittextNid.setText(it["nid"].toString())
                    binding.edittextMobile.setText(it["mobile"].toString())
                    binding.edittextDateOfBirth.setText(it["dob"].toString())
                    binding.editDropdownGender.setText(it["gender"].toString())
                    if (it["address"]!=null){
                        binding.edittextLocation.setText(it["address"].toString())
                        binding.edittextLocation.setEnabled(false)
                    }
                    if (it["nid"]!=null){
                        binding.edittextNid.setText(it["nid"].toString())
                        binding.edittextNid.setEnabled(false)
                    }
                }
            }.addOnFailureListener{
                Log.d("EditProfileSection", "Profile Data retrieve error : $it")
            }

            // using glide library to display the image
            storageRef.reference.child("user").child(userId.toString()).child("profile_photo").downloadUrl.addOnSuccessListener {
                if (it.toString().isNotEmpty()){
                    Glide.with(this)
                        .load(it)
                        .into(binding.editImage)
                }
            }.addOnFailureListener {
                Log.e("Firebase", "No Profile Picture yet")
            }

        }



        binding.editBtnBack.setOnClickListener{
            startActivity(Intent(this@EditProfileActivity,ProfileActivity::class.java))
            finish()
        }

        binding.edittextMobile.doOnTextChanged { text, start, before, count ->

            if (text!!.length>11){
                binding.edittextMobile.error = "Invalid Mobile No."
            }else if(text.length<11){
                binding.edittextMobile.error = null
            }

        }

        //Change Photo
        binding.editBtnCngImage.setOnClickListener{

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                    pickImageFromGallery()
                } else {
                    requestStoragePermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    pickImageFromGallery()
                } else {
                    requestStoragePermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }

        //Date of Birth Picker
        val birthDatePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select Your Date of Birth").build()
        birthDatePicker.addOnPositiveButtonClickListener {
            val birthDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(birthDatePicker.selection)
            binding.edittextDateOfBirth.setText(birthDate)
        }

        binding.edittextDateOfBirth.setOnClickListener{
            birthDatePicker.show(supportFragmentManager,"date_of_birth")
        }

        //Gender Drop Down Menu
        val gender = resources.getStringArray(R.array.gender)
        val arrayAdapter = ArrayAdapter(this,R.layout.gender_list_item,gender)
        binding.editDropdownGender.setAdapter(arrayAdapter)

        //Location picker
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        var lon =""
        var lat =""
        binding.edittextLocation.setOnClickListener{
            val task = fusedLocationProviderClient.lastLocation
            if (ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),101)
                return@setOnClickListener
            }
            task.addOnSuccessListener {
                Toast.makeText(this,"${it.longitude} ${it.latitude}",Toast.LENGTH_LONG).show()
                val geocoder = Geocoder(this,Locale.getDefault())
                val address: MutableList<Address>? = geocoder.getFromLocation(it.latitude,it.longitude,1)
                val cityName = address?.get(0)?.getAddressLine(0)
                lon = it.longitude.toString()
                lat = it.latitude.toString()
                Log.d("locationInfo", "AdminArea: "+address?.get(0)?.adminArea)
                Log.d("locationInfo", "Locality: "+address?.get(0)?.locality)
                Log.d("locationInfo", "Locale: "+address?.get(0)?.locale)
                Log.d("locationInfo", "SubLocality: "+address?.get(0)?.subLocality)
                Log.d("locationInfo", "featureName: "+address?.get(0)?.featureName)
                Log.d("locationInfo", "Phone: "+address?.get(0)?.phone)
                Log.d("locationInfo", "PostalCode: "+address?.get(0)?.postalCode)
                Log.d("locationInfo", "Premises: "+address?.get(0)?.premises)
                Log.d("locationInfo", "SubAdminArea: "+address?.get(0)?.subAdminArea)
                Log.d("locationInfo", "ThroughFare: "+address?.get(0)?.thoroughfare)
                Log.d("locationInfo", "SubThroughFare: "+address?.get(0)?.subThoroughfare)
                binding.edittextLocation.setText(cityName)
            }
        }



        binding.editBtnSave.setOnClickListener{
            if (TextUtils.isEmpty(binding.edittextLocation.text)){
                binding.edittextLocation.error = "Location is required."
            }else{
                binding.edittextLocation.error = null
            }

            if(TextUtils.isEmpty(binding.edittextFullname.text)){
                binding.edittextFullname.error = "Full name is required."
            }else{
                binding.edittextFullname.error = null
            }

            if(TextUtils.isEmpty(binding.edittextUsername.text)){
                binding.edittextUsername.error = "username is required."
            }else{
                binding.edittextUsername.error = null
            }

            if(TextUtils.isEmpty(binding.edittextNid.text)){
                binding.edittextNid.error = "NID Number is required."
            }else{
                binding.edittextNid.error = null
            }

            if(TextUtils.isEmpty(binding.edittextMobile.text)){
                binding.edittextMobile.error = "Mobile Number is required."
            }else{
                binding.edittextMobile.error = null
            }

            if(TextUtils.isEmpty(binding.edittextDateOfBirth.text)){
                binding.edittextDateOfBirth.error = "Date of birth is required."
            }else{
                binding.edittextDateOfBirth.error = null
            }

            if(TextUtils.isEmpty(binding.editDropdownGender.text)){
                binding.editDropdownGender.error = "Gender field is required."
            }else{
                binding.editDropdownGender.error = null
            }

            Log.d("insideerror", "onCreate: "+binding.edittextFullname.text+binding.edittextUsername.text+binding.edittextNid.text
                    +binding.edittextMobile.text+binding.edittextDateOfBirth.text+binding.editDropdownGender.text+binding.edittextLocation.text+lon+lat)


            if(binding.edittextLocation.error == null && binding.edittextFullname.error == null && binding.edittextUsername.error == null && binding.edittextNid.error == null
                && binding.edittextMobile.error == null && binding.edittextDateOfBirth.error == null && binding.editDropdownGender.error == null){


                val user = hashMapOf(
                    "fullname" to binding.edittextFullname.text.toString(),
                    "username" to binding.edittextUsername.text.toString(),
                    "nid" to binding.edittextNid.text.toString(),
                    "mobile" to binding.edittextMobile.text.toString(),
                    "dob" to binding.edittextDateOfBirth.text.toString(),
                    "gender" to binding.editDropdownGender.text.toString(),
                    "address" to binding.edittextLocation.text.toString(),
                    "longitude" to lon,
                    "latitude" to lat
                )


                if (userId != null) {
                    db.collection("user").document(userId).set(user).addOnSuccessListener {
                        Toast.makeText(this,"Profile Updated Successfully!",Toast.LENGTH_SHORT).show()


                        // On success, download the file URL and display it
                        storageRef.reference.child("user").child(userId.toString()).child("profile_photo").putFile(
                            imageUri!!
                        ).addOnFailureListener {
                            Log.e("Firebase", "Image Upload fail")
                        }

                        startActivity(Intent(this,ProfileActivity::class.java))
                        finish()
                    }.addOnFailureListener{
                        Toast.makeText(this,"update Process Failed.",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }

    private fun pickImageFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        photoLauncher.launch(galleryIntent)
    }

    override fun onResume() {
        super.onResume()
        imageUri?.let { uri ->
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        imageUri?.let { uri ->
            contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

}


