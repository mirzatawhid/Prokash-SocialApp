package com.dcoder.prokash.complaintSubmissionFragment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.dcoder.prokash.MainActivity
import com.dcoder.prokash.databinding.FragmentDetailsBinding
import com.dcoder.prokash.viewmodel.ComplaintSubmissionViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale


class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ComplaintSubmissionViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.detailAnonymous.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.detailAnonymous.text = "Anonymous"
            } else {
                binding.detailAnonymous.text = "Public"
            }
        }

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        // Set Current Date in EditText by Default
        val currentDate = Calendar.getInstance().time
        binding.detailDate.setText(dateFormat.format(currentDate))

        binding.detailDateLayout.setEndIconOnClickListener {
            showDatePicker()
        }

        binding.detailDate.setOnClickListener {
            showDatePicker()
        }

        binding.detailSubmit.setOnClickListener{
            if (TextUtils.isEmpty(binding.detailTitle.text)){
                binding.detailTitle.error = "Complaint Title is required."
            }else{
                binding.detailTitle.error = null
            }

            if(TextUtils.isEmpty(binding.detailDesc.text)){
                binding.detailDesc.error = "Complaint Description is required."
            }else{
                binding.detailDesc.error = null
            }
            if (binding.detailTitle.error == null && binding.detailDesc.error == null){
                viewModel.setTitle(binding.detailTitle.text.toString())
                viewModel.setDescription(binding.detailDesc.text.toString())
                viewModel.setAnonymous(binding.detailAnonymous.isChecked)
                viewModel.setDate(binding.detailDate.text.toString())
                Toast.makeText(requireContext(), "Every Input is Perfect.", Toast.LENGTH_SHORT).show()
                Log.d("ComplaintInputCheck", "lastFragment:\n" +
                        "title:${viewModel.title.value}\n" +
                        "Desc: ${viewModel.description.value}\n" +
                        "Date: ${viewModel.date.value}\n" +
                        "isImage: ${viewModel.isImage.value}\n" +
                        "evidence: ${viewModel.evidence.value}\n" +
                        "category: ${viewModel.category.value}\n" +
                        "subcategory: ${viewModel.subCategory.value}\n" +
                        "lon: ${viewModel.locationLongitude.value}\n" +
                        "lat: ${viewModel.locationLatitude.value}\n" +
                        "anonymous: ${viewModel.anonymous.value}\n")
                var userId ="anonymous"
                if(viewModel.anonymous.value == false){
                    val auth = FirebaseAuth.getInstance()
                    userId = auth.currentUser!!.uid
                }
                submitComplaint(
                    viewModel.evidence.value!!,viewModel.isImage.value!!,viewModel.title.value!!,viewModel.description.value!!,viewModel.category.value!!,
                    viewModel.subCategory.value!!,viewModel.date.value!!,viewModel.locationLatitude.value!!,viewModel.locationLongitude.value!!,viewModel.anonymous.value!!,userId)
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                requireActivity().finish()

            }
        }

    }

    private fun submitComplaint(
        uri: Uri,isImage:Boolean, title: String, description: String, category: String,subcategory: String,date:String,
        latitude: Double, longitude: Double,anonymous:Boolean,userId:String
    ) {
        uploadMediaToFirebase(uri, onSuccess = { mediaUrl ->
            saveComplaintToFirestore(title, description, category,subcategory,date, latitude, longitude,anonymous,userId,isImage, mediaUrl)
        }, onFailure = { exception ->
            Log.e("Firebase", "Upload failed: ${exception.message}")
        })
    }


    private fun uploadMediaToFirebase(uri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val storageRef = Firebase.storage.reference
        val fileRef = storageRef.child("complaints/${System.currentTimeMillis()}_${uri.lastPathSegment}")

        fileRef.putFile(uri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    onSuccess(downloadUrl.toString()) // Return URL of uploaded file
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception) // Handle failure
            }
    }

    private fun saveComplaintToFirestore(
        title: String,
        description: String,
        category: String,
        subcategory: String,
        date: String,
        latitude: Double,
        longitude: Double,
        anonymous: Boolean,
        userId: String,
        isImage: Boolean,
        mediaUrl: String // URL of uploaded file
    ) {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedDate = current.format(formatter)
        val db = Firebase.firestore
        val complaintData = hashMapOf(
            "title" to title,
            "description" to description,
            "category" to category,
            "sub_category" to subcategory,
            "latitude" to latitude,
            "longitude" to longitude,
            "anonymous" to anonymous,
            "user_id" to userId,
            "is_image" to isImage,
            "mediaUrl" to mediaUrl,
            "date" to date,
            "upload_date" to formattedDate
        )

        db.collection("complaints")
            .add(complaintData)
            .addOnSuccessListener {
                Log.d("Firestore", "Complaint saved successfully!")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error saving complaint", e)
            }
    }



    private fun showDatePicker() {
        // Create a MaterialDatePicker instance
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select a Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        // Show the Date Picker
        datePicker.show(parentFragmentManager, "datePicker")

        // Handle Date Selection
        datePicker.addOnPositiveButtonClickListener { selectedDate ->
            // Format the selected date
            val formattedDate =
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(selectedDate))
            // Display the selected date in the EditText
            binding.detailDate.setText(formattedDate)
        }


    }
}