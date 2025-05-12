package com.example.docscanner.ui.camera

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.docscanner.R
import com.example.docscanner.databinding.ActivityCameraBinding
import com.example.docscanner.ui.edit.EditImageActivity
import com.example.docscanner.util.ImageProcessor
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
    private val imageProcessor = ImageProcessor()
    private var isDocumentDetected = false

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
        // Animate the UI elements when they appear
        binding.topControls.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))
        binding.bottomControls.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))

        binding.buttonCapture.apply {
            setOnClickListener { 
                startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_press))
                takePhoto()
            }
        }
        
        binding.buttonFlash.apply {
            setOnClickListener {
                startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_press))
                cycleFlashMode()
            }
        }
        
        binding.buttonClose.apply {
            setOnClickListener {
                startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_press))
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left)
            }
        }

        binding.gridToggle.apply {
            setOnClickListener {
                startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_press))
                toggleGrid()
            }
        }

        binding.buttonGallery.apply {
            setOnClickListener {
                startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_press))
                // TODO: Implement gallery picker
            }
        }

        binding.buttonBatch.apply {
            setOnClickListener {
                startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_press))
                // TODO: Implement batch mode
            }
        }

        updateFlashIcon()
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

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        // Detect document edges
                        detectDocumentEdges(imageProxy)
                        imageProxy.close()
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun detectDocumentEdges(imageProxy: ImageProxy) {
        // TODO: Implement edge detection using OpenCV
        // For now, just simulate detection
        if (!isDocumentDetected) {
            isDocumentDetected = true
            runOnUiThread {
                binding.edgeDetectionOverlay.setDocumentDetected(true)
                showDetectionStatus("Document detected")
            }
        }
    }

    private fun showDetectionStatus(message: String) {
        binding.textDetectionStatus.apply {
            text = message
            visibility = View.VISIBLE
            startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
            postDelayed({
                startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out))
                visibility = View.GONE
            }, 2000)
        }
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
                    overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
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
            ImageCapture.FLASH_MODE_AUTO -> R.drawable.ic_flash_auto
            ImageCapture.FLASH_MODE_ON -> android.R.drawable.ic_menu_camera // TODO: Add custom flash on icon
            else -> android.R.drawable.ic_menu_camera // TODO: Add custom flash off icon
        }
        binding.buttonFlash.setImageResource(iconResource)
    }

    private fun toggleGrid() {
        binding.gridOverlay.apply {
            visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (visibility == View.VISIBLE) {
                startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in))
            }
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
