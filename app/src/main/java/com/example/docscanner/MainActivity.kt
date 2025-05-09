package com.example.docscanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.docscanner.databinding.ActivityMainBinding
import com.example.docscanner.ui.camera.CameraActivity
import com.example.docscanner.ui.documents.DocumentsAdapter
import com.example.docscanner.model.Document

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var documentsAdapter: DocumentsAdapter
    private val PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val PERMISSION_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        // Setup RecyclerView
        documentsAdapter = DocumentsAdapter(emptyList()) { document ->
            // Handle document click
            openDocument(document)
        }
        
        binding.recyclerDocuments.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = documentsAdapter
        }

        // Setup FAB
        binding.fabScan.setOnClickListener {
            if (checkCameraPermission()) {
                startCamera()
            } else {
                requestPermissions()
            }
        }

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }

    private fun checkPermissions() {
        val permissionsToRequest = PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE)
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
    }

    private fun openDocument(document: Document) {
        // TODO: Implement document opening logic
        Toast.makeText(this, "Opening ${document.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions granted
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions required to use the app", Toast.LENGTH_LONG).show()
            }
        }
    }
}
