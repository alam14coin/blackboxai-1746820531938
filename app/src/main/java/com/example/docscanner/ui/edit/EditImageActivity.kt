package com.example.docscanner.ui.edit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.docscanner.R
import com.example.docscanner.databinding.ActivityEditImageBinding
import com.example.docscanner.util.ImageProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import java.io.File

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
        
        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "Failed to initialize OpenCV", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupUI()
        loadImage()
    }
    
    private fun setupUI() {
        binding.toolbar.apply {
            setNavigationOnClickListener { onBackPressed() }
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
        
        binding.buttonRotate.setOnClickListener {
            rotateImage()
        }
        
        binding.buttonCrop.setOnClickListener {
            toggleCropMode()
        }
        
        binding.buttonConfirm.setOnClickListener {
            confirmCrop()
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
        // TODO: Implement PDF generation and saving
        Toast.makeText(this, "PDF generation will be implemented", Toast.LENGTH_SHORT).show()
    }
    
    enum class FilterType {
        ORIGINAL,
        BLACK_AND_WHITE,
        GRAYSCALE,
        ENHANCE
    }
}
