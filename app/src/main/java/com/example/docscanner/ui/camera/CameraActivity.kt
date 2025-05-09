package com.example.docscanner.ui.camera

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.docscanner.databinding.ActivityCameraBinding
import com.example.docscanner.ui.edit.EditImageActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var flashMode = ImageCapture.FLASH_MODE_AUTO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        startCamera()
    }

    private fun setupUI() {
        binding.buttonCapture.setOnClickListener { takePhoto() }
        
        binding.buttonFlash.setOnClickListener {
            cycleFlashMode()
        }
        
        binding.buttonClose.setOnClickListener {
            finish()
        }

        binding.gridToggle.setOnClickListener {
            binding.gridOverlay.visibility = if (binding.gridOverlay.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setFlashMode(flashMode)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val intent = Intent(this@CameraActivity, EditImageActivity::class.java).apply {
                        putExtra("image_path", photoFile.absolutePath)
                    }
                    startActivity(intent)
                    finish()
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        this@CameraActivity,
                        "Failed to capture image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun cycleFlashMode() {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
        
        updateFlashIcon()
        imageCapture?.flashMode = flashMode
    }

    private fun updateFlashIcon() {
        val iconResource = when (flashMode) {
            ImageCapture.FLASH_MODE_AUTO -> android.R.drawable.ic_menu_camera
            ImageCapture.FLASH_MODE_ON -> android.R.drawable.ic_menu_camera
            else -> android.R.drawable.ic_menu_camera
        }
        binding.buttonFlash.setImageResource(iconResource)
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(com.example.docscanner.R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
