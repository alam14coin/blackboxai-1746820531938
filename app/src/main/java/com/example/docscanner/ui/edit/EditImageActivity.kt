package com.example.docscanner.ui.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.docscanner.R
import com.example.docscanner.databinding.ActivityEditImageBinding
import com.example.docscanner.util.ImageProcessor
import com.example.docscanner.util.PDFGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EditImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditImageBinding
    private var originalBitmap: Bitmap? = null
    private var currentBitmap: Bitmap? = null
    private lateinit var imagePath: String
    private val imageProcessor = ImageProcessor()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        loadImage()
    }
    
    private fun setupUI() {
        // Animate toolbar and controls when they appear
        binding.toolbar.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))
        binding.controlsContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))

        binding.toolbar.apply {
            setNavigationOnClickListener { 
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left)
            }
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_save -> {
                        savePDF()
                        true
                    }
                    else -> false
                }
            }
        }
        
        binding.filterGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.filterOriginal -> applyFilter(FilterType.ORIGINAL)
                R.id.filterBW -> applyFilter(FilterType.BLACK_AND_WHITE)
                R.id.filterGray -> applyFilter(FilterType.GRAYSCALE)
                R.id.filterEnhance -> applyFilter(FilterType.ENHANCE)
            }
        }
        
        binding.buttonRotate.apply {
            setOnClickListener {
                startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_press))
                rotateImage()
            }
        }
        
        binding.buttonCrop.apply {
            setOnClickListener {
                startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_press))
                toggleCropMode()
            }
        }
        
        binding.buttonConfirm.apply {
            setOnClickListener {
                startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_press))
                confirmCrop()
            }
        }
    }
    
    private fun loadImage() {
        imagePath = intent.getStringExtra("image_path") ?: run {
            Toast.makeText(this, "No image path provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                originalBitmap = BitmapFactory.decodeFile(imagePath)
                currentBitmap = originalBitmap?.copy(originalBitmap!!.config, true)
                
                withContext(Dispatchers.Main) {
                    binding.imageView.setImageBitmap(currentBitmap)
                    binding.imageView.startAnimation(AnimationUtils.loadAnimation(this@EditImageActivity, R.anim.fade_in))
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditImageActivity, "Failed to load image", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
    
    private fun applyFilter(filterType: FilterType) {
        binding.progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val filteredBitmap = when (filterType) {
                    FilterType.ORIGINAL -> originalBitmap?.copy(originalBitmap!!.config, true)
                    FilterType.BLACK_AND_WHITE -> imageProcessor.applyBlackAndWhite(currentBitmap!!)
                    FilterType.GRAYSCALE -> imageProcessor.applyGrayscale(currentBitmap!!)
                    FilterType.ENHANCE -> imageProcessor.enhanceDocument(currentBitmap!!)
                }
                
                withContext(Dispatchers.Main) {
                    currentBitmap = filteredBitmap
                    binding.imageView.setImageBitmap(currentBitmap)
                    binding.imageView.startAnimation(AnimationUtils.loadAnimation(this@EditImageActivity, R.anim.fade_in))
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditImageActivity, "Failed to apply filter", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
    
    private fun rotateImage() {
        binding.progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rotatedBitmap = imageProcessor.rotateImage(currentBitmap!!, 90f)
                
                withContext(Dispatchers.Main) {
                    currentBitmap = rotatedBitmap
                    binding.imageView.setImageBitmap(currentBitmap)
                    binding.imageView.startAnimation(AnimationUtils.loadAnimation(this@EditImageActivity, R.anim.fade_in))
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditImageActivity, "Failed to rotate image", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
    
    private fun toggleCropMode() {
        binding.cropOverlay.visibility = View.VISIBLE
        binding.cropOverlay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in))
        binding.buttonConfirm.visibility = View.VISIBLE
        binding.buttonCrop.visibility = View.GONE
    }
    
    private fun confirmCrop() {
        val corners = binding.cropOverlay.getCorners()
        binding.progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val croppedBitmap = imageProcessor.cropPerspective(currentBitmap!!, corners)
                
                withContext(Dispatchers.Main) {
                    currentBitmap = croppedBitmap
                    binding.imageView.setImageBitmap(currentBitmap)
                    binding.imageView.startAnimation(AnimationUtils.loadAnimation(this@EditImageActivity, R.anim.fade_in))
                    binding.cropOverlay.visibility = View.GONE
                    binding.buttonConfirm.visibility = View.GONE
                    binding.buttonCrop.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditImageActivity, "Failed to crop image", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
    
    private fun savePDF() {
        binding.progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileName = "DOC_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
                val outputFile = File(getExternalFilesDir(null), fileName)
                
                val success = PDFGenerator.createPDF(
                    this@EditImageActivity,
                    listOf(currentBitmap!!),
                    outputFile.absolutePath
                )
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@EditImageActivity, "PDF saved successfully", Toast.LENGTH_SHORT).show()
                        finish()
                        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left)
                    } else {
                        Toast.makeText(this@EditImageActivity, "Failed to save PDF", Toast.LENGTH_SHORT).show()
                    }
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditImageActivity, "Failed to save PDF", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }
    
    enum class FilterType {
        ORIGINAL,
        BLACK_AND_WHITE,
        GRAYSCALE,
        ENHANCE
    }
}
