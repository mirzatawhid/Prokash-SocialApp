package com.dcoder.prokash

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.dcoder.prokash.complaintSubmissionFragment.CategoryFragment
import com.dcoder.prokash.complaintSubmissionFragment.EvidenceFragment
import com.dcoder.prokash.databinding.ActivityComplaintSubmissionBinding

class ComplaintSubmissionActivity : AppCompatActivity() {
    private lateinit var binding:ActivityComplaintSubmissionBinding
    private var selectedTab = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComplaintSubmissionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val anim = AnimationUtils.loadAnimation(this,R.anim.complaint_submit_bar_anim)
        anim.fillAfter=true

        replaceFragment(EvidenceFragment())
        if (!checkPermissions()) {
            requestPermissions()
        }



        binding.evidenceLayout.setOnClickListener {
            if (selectedTab != 1) {
                binding.evidenceImage.setImageResource(R.drawable.ic_complaint_submit_bar_evidence_selected)
                binding.categoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_category)
                binding.subcategoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_subcategory)
                binding.locationImage.setImageResource(R.drawable.ic_complaint_submit_bar_location)
                binding.detailImage.setImageResource(R.drawable.ic_complaint_submit_bar_detail)

                binding.evidenceTxt.visibility = View.VISIBLE
                binding.categoryTxt.visibility = View.GONE
                binding.subcategoryTxt.visibility = View.GONE
                binding.locationTxt.visibility = View.GONE
                binding.detailTxt.visibility = View.GONE

                binding.evidenceLayout.setBackgroundResource(R.drawable.complaint_submission_bar_round_back)
                binding.categoryLayout.setBackgroundResource(android.R.color.transparent)
                binding.subcategoryLayout.setBackgroundResource(android.R.color.transparent)
                binding.locationLayout.setBackgroundResource(android.R.color.transparent)
                binding.detailLayout.setBackgroundResource(android.R.color.transparent)

                binding.evidenceLayout.startAnimation(anim)

                replaceFragment(EvidenceFragment())

                selectedTab = 1

            }
        }

        binding.categoryLayout.setOnClickListener {
            if (selectedTab != 2) {
                binding.evidenceImage.setImageResource(R.drawable.ic_complaint_submit_bar_evidence)
                binding.categoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_category_selected)
                binding.subcategoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_subcategory)
                binding.locationImage.setImageResource(R.drawable.ic_complaint_submit_bar_location)
                binding.detailImage.setImageResource(R.drawable.ic_complaint_submit_bar_detail)

                binding.evidenceTxt.visibility = View.GONE
                binding.categoryTxt.visibility = View.VISIBLE
                binding.subcategoryTxt.visibility = View.GONE
                binding.locationTxt.visibility = View.GONE
                binding.detailTxt.visibility = View.GONE

                binding.evidenceLayout.setBackgroundResource(android.R.color.transparent)
                binding.categoryLayout.setBackgroundResource(R.drawable.complaint_submission_bar_round_back)
                binding.subcategoryLayout.setBackgroundResource(android.R.color.transparent)
                binding.locationLayout.setBackgroundResource(android.R.color.transparent)
                binding.detailLayout.setBackgroundResource(android.R.color.transparent)

                binding.categoryLayout.startAnimation(anim)

                replaceFragment(CategoryFragment())

                selectedTab = 2

            }
        }

        binding.subcategoryLayout.setOnClickListener{
            if (selectedTab!=3){
                binding.evidenceImage.setImageResource(R.drawable.ic_complaint_submit_bar_evidence)
                binding.categoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_category)
                binding.subcategoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_subcategory_selected)
                binding.locationImage.setImageResource(R.drawable.ic_complaint_submit_bar_location)
                binding.detailImage.setImageResource(R.drawable.ic_complaint_submit_bar_detail)

                binding.evidenceTxt.visibility=View.GONE
                binding.categoryTxt.visibility=View.GONE
                binding.subcategoryTxt.visibility=View.VISIBLE
                binding.locationTxt.visibility=View.GONE
                binding.detailTxt.visibility=View.GONE

                binding.evidenceLayout.setBackgroundResource(android.R.color.transparent)
                binding.categoryLayout.setBackgroundResource(android.R.color.transparent)
                binding.subcategoryLayout.setBackgroundResource(R.drawable.complaint_submission_bar_round_back)
                binding.locationLayout.setBackgroundResource(android.R.color.transparent)
                binding.detailLayout.setBackgroundResource(android.R.color.transparent)

                binding.subcategoryLayout.startAnimation(anim)

                selectedTab = 3

            }
        }

        binding.locationLayout.setOnClickListener{
            if (selectedTab!=4){
                binding.evidenceImage.setImageResource(R.drawable.ic_complaint_submit_bar_evidence)
                binding.categoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_category)
                binding.subcategoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_subcategory)
                binding.locationImage.setImageResource(R.drawable.ic_complaint_submit_bar_location_selected)
                binding.detailImage.setImageResource(R.drawable.ic_complaint_submit_bar_detail)

                binding.evidenceTxt.visibility=View.GONE
                binding.categoryTxt.visibility=View.GONE
                binding.subcategoryTxt.visibility=View.GONE
                binding.locationTxt.visibility=View.VISIBLE
                binding.detailTxt.visibility=View.GONE

                binding.evidenceLayout.setBackgroundResource(android.R.color.transparent)
                binding.categoryLayout.setBackgroundResource(android.R.color.transparent)
                binding.subcategoryLayout.setBackgroundResource(android.R.color.transparent)
                binding.locationLayout.setBackgroundResource(R.drawable.complaint_submission_bar_round_back)
                binding.detailLayout.setBackgroundResource(android.R.color.transparent)

                binding.locationLayout.startAnimation(anim)

                selectedTab = 4

            }
        }

        binding.detailLayout.setOnClickListener{
            if (selectedTab!=5){
                binding.evidenceImage.setImageResource(R.drawable.ic_complaint_submit_bar_evidence)
                binding.categoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_category)
                binding.subcategoryImage.setImageResource(R.drawable.ic_complaint_submit_bar_subcategory)
                binding.locationImage.setImageResource(R.drawable.ic_complaint_submit_bar_location)
                binding.detailImage.setImageResource(R.drawable.ic_complaint_submit_bar_detail_selected)

                binding.evidenceTxt.visibility=View.GONE
                binding.categoryTxt.visibility=View.GONE
                binding.subcategoryTxt.visibility=View.GONE
                binding.locationTxt.visibility=View.GONE
                binding.detailTxt.visibility=View.VISIBLE

                binding.evidenceLayout.setBackgroundResource(android.R.color.transparent)
                binding.categoryLayout.setBackgroundResource(android.R.color.transparent)
                binding.subcategoryLayout.setBackgroundResource(android.R.color.transparent)
                binding.locationLayout.setBackgroundResource(android.R.color.transparent)
                binding.detailLayout.setBackgroundResource(R.drawable.complaint_submission_bar_round_back)

                binding.detailLayout.startAnimation(anim)
                selectedTab = 5

            }
        }

        binding.submitBtnBack.setOnClickListener{
            startActivity(Intent(this@ComplaintSubmissionActivity,MainActivity::class.java))
            finish()
        }

    }

    private fun requestPermissions() {
        requestPermissions(
            arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),102
        )
    }

    private fun checkPermissions(): Boolean {

        val cameraPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        )
        val storagePermission = ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return cameraPermission == PackageManager.PERMISSION_GRANTED && storagePermission == PackageManager.PERMISSION_GRANTED

    }

    private fun replaceFragment(fragment : Fragment){
        val fragmentTransition = supportFragmentManager.beginTransaction().replace(R.id.fragment_container,fragment).commit()
    }
}