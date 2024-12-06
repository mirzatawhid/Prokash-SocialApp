package com.dcoder.prokash.complaintSubmissionFragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.dcoder.prokash.databinding.FragmentDetailsBinding
import com.dcoder.prokash.viewmodel.ComplaintSubmissionViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ComplaintSubmissionViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        viewModel = ViewModelProvider(requireActivity()).get(ComplaintSubmissionViewModel::class.java)
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
            }
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