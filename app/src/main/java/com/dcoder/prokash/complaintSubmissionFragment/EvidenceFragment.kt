package com.dcoder.prokash.complaintSubmissionFragment

import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.dcoder.prokash.R
import com.dcoder.prokash.databinding.FragmentEvidenceBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class EvidenceFragment : Fragment() {

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private var _binding: FragmentEvidenceBinding? = null
    private val binding get() = _binding!!
    private var imageCapture: ImageCapture? = null
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var cameraExecutor: ExecutorService
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var currentRecording: Recording? = null

    //camera permission launcher
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Camera permission granted
            startCamera()
        } else {
            // Camera permission denied
        }
    }

    //Audio permission Launcher
    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Record audio permission granted
        } else {
            // Record audio permission denied
        }
    }

    //both permission launcher
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraPermissionGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val audioPermissionGranted = permissions[android.Manifest.permission.RECORD_AUDIO] ?: false
        val storagePermissionGranted = permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        val rImagePermissionGranted = permissions[if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        }] ?: false
        val rVideoPermissionGranted = permissions[if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_VIDEO
        } else {
            permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        }] ?: false

        if (cameraPermissionGranted && audioPermissionGranted && storagePermissionGranted) {
            startCamera()
        } else if (cameraPermissionGranted && audioPermissionGranted && rVideoPermissionGranted && rImagePermissionGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Permissions not granted.", Toast.LENGTH_SHORT).show()
            Log.d("permissionList", "Permissions list: $permissions")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvidenceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (checkPermissions()) {
            startCamera()
        } else {
            if (Build.VERSION.SDK_INT >= 33) {
                requestMultiplePermissionsLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.READ_MEDIA_VIDEO,
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    )
                )
            } else {
                requestMultiplePermissionsLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }

        binding.captureButton.setOnClickListener { takePhoto() }
        binding.recordButton.setOnClickListener { captureVideo() }
        binding.switchCameraButton.setOnClickListener { switchCamera() }
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    private fun captureVideo() {
        val videoFile = File(
            getOutputDirectory(),
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".mp4"
        )

        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        if (currentRecording != null) {
            currentRecording?.stop()
            currentRecording = null
            binding.recordButton.text = "Record"
        } else {
            currentRecording = videoCapture.output
                .prepareRecording(requireContext(), outputOptions)
                .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            binding.recordButton.text = "Stop Recording"
                        }
                        is VideoRecordEvent.Finalize -> {
                            val msg = "Video capture succeeded: ${videoFile.absolutePath}"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            Log.d("CameraXApp", msg)
                            binding.recordButton.text = "Record"
                        }
                    }
                }
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            getOutputDirectory(),
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, photoFile.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Prokash")
            }else {
                put(MediaStore.Images.Media.DATA, photoFile.absolutePath)
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraXApp", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    Log.d("CameraXApp", "Photo capture succeeded: $savedUri")
                }
            })
    }

    private fun checkPermissions(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.CAMERA
        )
        val storagePermission = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val audioPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.RECORD_AUDIO
        )

        return if (Build.VERSION.SDK_INT < 33) {
            cameraPermission == PackageManager.PERMISSION_GRANTED &&
                    storagePermission == PackageManager.PERMISSION_GRANTED &&
                    audioPermission == PackageManager.PERMISSION_GRANTED
        } else {
            cameraPermission == PackageManager.PERMISSION_GRANTED &&
                    audioPermission == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val recorder = Recorder.Builder().setExecutor(cameraExecutor).build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture, videoCapture
                )
            } catch (exc: Exception) {
                Log.e("CameraXApp", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun getOutputDirectory(): String {
        val mediaDir = activity?.externalMediaDirs?.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir.absolutePath else requireContext().filesDir.absolutePath
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }
}
