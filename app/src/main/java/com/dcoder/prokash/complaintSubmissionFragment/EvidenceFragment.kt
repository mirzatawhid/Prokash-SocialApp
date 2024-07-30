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
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.dcoder.prokash.R
import com.dcoder.prokash.viewmodel.ComplaintSubmissionViewModel
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
    private var player: ExoPlayer? = null


    private lateinit var viewModel: ComplaintSubmissionViewModel


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
        viewModel = ViewModelProvider(requireActivity()).get(ComplaintSubmissionViewModel::class.java)
        return binding.root
    }





    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if(viewModel.evidence.value != null){
            showPreview(viewModel.evidence.value!!,viewModel.isImage.value!!)
        }else{

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
            getTempOutputDirectory(),
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
                            showPreview(Uri.fromFile(videoFile), false)

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
            getTempOutputDirectory(),
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
                    showPreview(Uri.fromFile(photoFile), true)
                    val msg = "Photo capture succeeded: $savedUri /n Location: ${savedUri.path}"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    Log.d("CameraXApp", "Photo capture succeeded: $msg")
                }
            })
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




    private fun showPreview(uri: Uri, isImage: Boolean) {
        binding.viewFinder.visibility = View.GONE
        binding.previewLayout.visibility = View.VISIBLE
        binding.imagePreview.visibility = if (isImage) View.VISIBLE else View.GONE
        binding.videoPreview.visibility = if (!isImage) View.VISIBLE else View.GONE
        binding.previewButtons.visibility = View.VISIBLE
        binding.captureButton.visibility = View.GONE
        binding.recordButton.visibility = View.GONE
        binding.switchCameraButton.visibility = View.GONE

        if (isImage) {
            binding.imagePreview.setImageURI(uri)
        } else {
            setupVideoPlayer(uri)
        }

        binding.discardButton.setOnClickListener {
            discardMedia(uri)
            resetPreview()
        }

        binding.saveButton.setOnClickListener {
            saveMedia(uri, isImage)
            goToNext(uri, isImage)
        }

        binding.nextButton.setOnClickListener {
            goToNext(uri, isImage)
        }

    }





    private fun setupVideoPlayer(uri: Uri) {
        Log.d("setupVideo", "setupVideoPlayer: "+uri)
        player = ExoPlayer.Builder(requireContext()).build().also {
            binding.videoPreview.player = it
            val mediaItem = MediaItem.fromUri(uri)
            it.setMediaItem(mediaItem)
            it.prepare()
            it.play()
        }
    }




    private fun saveMedia(uri: Uri, isImage: Boolean) {
        val displayName = uri.lastPathSegment
        val folderName = "ProkashMedia"
        val mimeType = if (isImage) "image/jpeg" else "video/mp4"
        val contentUri = if (isImage) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val resolver = requireContext().contentResolver
        var mediaExists = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Query to check if the media already exists for API level 29 and above
            val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf(displayName, Environment.DIRECTORY_DOCUMENTS + "/$folderName/")

            resolver.query(contentUri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    mediaExists = true
                }
            }
        } else {
            // Query to check if the media already exists for API levels below 29
            val storageDir = Environment.getExternalStorageDirectory().toString() + "/$folderName"
            val file = File(storageDir, displayName!!)
            mediaExists = file.exists()
        }

        if (mediaExists) {
            Toast.makeText(requireContext(), "Media already exists in the gallery", Toast.LENGTH_SHORT).show()
        } else {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/$folderName")
                } else {
                    val storageDir = Environment.getExternalStorageDirectory().toString() + "/$folderName"
                    val file = File(storageDir)
                    if (!file.exists()) {
                        file.mkdirs()
                    }
                    put(MediaStore.MediaColumns.DATA, "$storageDir/$displayName")
                }
            }

            resolver.insert(contentUri, contentValues)?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    resolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }

            viewModel.setEvidence(uri)
            viewModel.setSelectedTab(2)
            viewModel.setIsImage(isImage)
            Toast.makeText(requireContext(), "Media saved to gallery", Toast.LENGTH_SHORT).show()

            replaceFragment(CategoryFragment())
        }
    }


    private fun discardMedia(uri: Uri) {
        val file = File(uri.path!!)
        if (file.exists()) {
            val deleted = file.delete()
            if (deleted) {
                Toast.makeText(requireContext(), "Media discarded", Toast.LENGTH_SHORT).show()
                viewModel.setEvidence(null)
                viewModel.setIsImage(null)
            } else {
                Toast.makeText(requireContext(), "Failed to discard media", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show()
        }

    }

    private fun goToNext(uri: Uri, isImage: Boolean){
        viewModel.setEvidence(uri)
        viewModel.setSelectedTab(2)
        viewModel.setIsImage(isImage)

        replaceFragment(CategoryFragment())
    }

    private fun resetPreview() {
        binding.viewFinder.visibility = View.VISIBLE
        binding.previewLayout.visibility = View.GONE
        binding.imagePreview.visibility = View.GONE
        binding.videoPreview.visibility = View.GONE
        binding.previewButtons.visibility = View.GONE
        binding.captureButton.visibility = View.VISIBLE
        binding.recordButton.visibility = View.VISIBLE
        binding.switchCameraButton.visibility = View.VISIBLE
        startCamera()
    }




    private fun getOutputDirectory(): String {
        val mediaDir = activity?.externalMediaDirs?.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir.absolutePath else requireContext().filesDir.absolutePath
    }


    private fun getTempOutputDirectory(): File {
        val mediaDir = requireContext().externalCacheDirs.firstOrNull()?.let {
            File(it, "temp").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else requireContext().cacheDir
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

    private fun replaceFragment(fragment : Fragment){
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }
}
