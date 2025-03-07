package com.dcoder.prokash.complaintSubmissionFragment

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.dcoder.prokash.R
import com.dcoder.prokash.viewmodel.ComplaintSubmissionViewModel
import com.dcoder.prokash.databinding.FragmentEvidenceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var isRecordingPaused = false


    private val viewModel: ComplaintSubmissionViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvidenceBinding.inflate(inflater, container, false)
        return binding.root
    }


    // In onViewCreated() method, check and request permissions and open camera
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (viewModel.evidence.value != null) {
            showPreview(viewModel.evidence.value!!, viewModel.isImage.value!!)
        } else {
            checkAndRequestPermissions()
        }

        binding.captureButton.setOnClickListener { takePhoto() }
        binding.recordButton.setOnClickListener { captureVideo() }
        binding.pauseResumeButton.setOnClickListener { pauseResumeRecording() }
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
            isRecordingPaused = false
            binding.recordButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.complaint_submission_evidence_video)
            binding.pauseResumeButton.visibility = View.GONE
            binding.captureButton.visibility = View.VISIBLE
        } else {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                return
            }
            currentRecording = videoCapture.output
                .prepareRecording(requireContext(), outputOptions).withAudioEnabled()
                .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            binding.recordButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.complaint_submission_evidence_stop)
                            binding.captureButton.visibility= View.GONE
                            binding.pauseResumeButton.visibility = View.VISIBLE
                        }
                        is VideoRecordEvent.Finalize -> {
                            val msg = "Video capture succeeded: ${videoFile.absolutePath}"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            showPreview(Uri.fromFile(videoFile), false)
                            Log.d("CameraXApp", msg)
                            binding.recordButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.complaint_submission_evidence_video)
                            binding.pauseResumeButton.visibility = View.GONE
                            //binding.captureButton.visibility = View.VISIBLE
                        }
                    }
                }
        }
    }



    private fun pauseResumeRecording() {
        if (currentRecording != null) {
            if (isRecordingPaused) {
                currentRecording?.resume()
                binding.pauseResumeButton.background = ContextCompat.getDrawable(requireContext(),R.drawable.complaint_submission_evidence_pause)
            } else {
                currentRecording?.pause()
                binding.pauseResumeButton.background = ContextCompat.getDrawable(requireContext(),R.drawable.complaint_submission_evidence_resume)
            }
            isRecordingPaused = !isRecordingPaused
        }
    }



    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            getTempOutputDirectory(),
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        // Use file:// URI scheme directly
        val photoUri = Uri.fromFile(photoFile)

        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(photoFile).build(),
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraXApp", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Use the URI with file:// scheme
                    showPreview(photoUri, true)
                    val msg = "Photo capture succeeded: $photoUri /n Location: ${photoUri.path}"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    Log.d("CameraXApp", "Photo capture succeeded: $msg")
                }
            }
        )
    }



    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.viewFinder.surfaceProvider
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

        binding.recordButton.visibility = View.GONE
        binding.switchCameraButton.visibility = View.GONE
        binding.galleryButton.visibility = View.GONE
        binding.captureButton.visibility = View.GONE

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
        Log.d("setupVideo", "setupVideoPlayer: $uri")
        player = ExoPlayer.Builder(requireContext()).build().also {
            binding.videoPreview.player = it
            val mediaItem = MediaItem.fromUri(uri)
            it.setMediaItem(mediaItem)
            it.prepare()
            it.play()
        }
    }


    private fun saveMedia(uri: Uri, isImage: Boolean) {
        val context = context?.applicationContext ?: return  // Avoid using requireContext()
        val resolver = context.contentResolver
        val folderName = "PrakashMedia"
        val mimeType = if (isImage) "image/jpeg" else "video/mp4"
        val mediaCollection =
            if (isImage) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val displayName = "IMG_${System.currentTimeMillis()}.jpg".takeIf { isImage }
            ?: "VID_${System.currentTimeMillis()}.mp4"

        CoroutineScope(Dispatchers.IO).launch {
            var mediaExists = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf(displayName, Environment.DIRECTORY_PICTURES + "/$folderName/")

                resolver.query(mediaCollection, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), selection, selectionArgs, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            mediaExists = true
                        }
                    }
            } else {
                val storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderName)
                val file = File(storageDir, displayName)
                mediaExists = file.exists()
            }

            if (mediaExists) {
                withContext(Dispatchers.Main) {
                    context.let {
                        Toast.makeText(it, "Media already exists", Toast.LENGTH_SHORT).show()
                    }
                }
                return@launch
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/$folderName")
                } else {
                    val storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderName)
                    if (!storageDir.exists()) storageDir.mkdirs()
                    put(MediaStore.MediaColumns.DATA, File(storageDir, displayName).absolutePath)
                }
            }

            val newUri = resolver.insert(mediaCollection, contentValues)
            if (newUri != null) {
                try {
                    resolver.openOutputStream(newUri)?.use { outputStream ->
                        resolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        context.let {
                            Toast.makeText(it, "Media saved to gallery", Toast.LENGTH_SHORT).show()
                        }
                    }

                    //File(uri.path!!).takeIf { it.exists() }?.delete()

                    withContext(Dispatchers.Main) {
                        if (isAdded) {  // Only call replaceFragment() if fragment is still attached
                            viewModel.setEvidence(uri)
                            viewModel.setSelectedTab(2)
                            viewModel.setIsImage(isImage)
                            replaceFragment(CategoryFragment())
                        } else {
                            Log.e("SaveMediaError", "Fragment was detached before replaceFragment()")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        context.let {
                            Toast.makeText(it, "Error saving media: ${e.message}", Toast.LENGTH_SHORT).show()

                        }
                        Log.e("SaveMediaError", "Exception while saving media", e)
                    }
                    e.printStackTrace()
                }
            } else {
                withContext(Dispatchers.Main) {
                    context.let {
                        Toast.makeText(it, "Failed to create media file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    private fun discardMedia(uri: Uri) {
        if (uri.scheme == "file") {
            val file = File(uri.path!!)
            if (file.exists()) {
                file.delete()
                viewModel.setEvidence(null)
                viewModel.setIsImage(null)
                Toast.makeText(requireContext(), "Media Discarded Successfully.", Toast.LENGTH_SHORT).show()
            }
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
        binding.galleryButton.visibility = View.VISIBLE
        startCamera()
    }



    private fun getTempOutputDirectory(): File {
        val mediaDir = requireContext().externalCacheDirs.firstOrNull()?.let {
            File(it, "temp").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else requireContext().cacheDir
    }


    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = permissions.filter { !it.value }.keys

        if (deniedPermissions.isEmpty()) {
            // All permissions granted, proceed
            startCamera()
        } else {
            // Check if the user selected "Don't Ask Again"
            val shouldShowRationale = deniedPermissions.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)
            }

            if (!shouldShowRationale) {
                // User selected "Don't Ask Again" -> Show a settings dialog
                showSettingsDialog()
            } else {
                // Permissions denied without "Don't Ask Again", show a toast or explanation
                Toast.makeText(requireContext(), "Permissions are required for this feature", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permissions Required")
            .setMessage("This feature requires permissions. Please enable them in settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }



    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) { // Android 12 or lower
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else { // Android 13+
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        }

        permissionLauncher.launch(permissions.toTypedArray())
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
