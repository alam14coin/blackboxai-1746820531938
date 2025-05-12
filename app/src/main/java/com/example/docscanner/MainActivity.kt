package com.example.docscanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.docscanner.databinding.ActivityMainBinding
import com.example.docscanner.ui.camera.CameraActivity
import com.example.docscanner.ui.documents.DocumentsAdapter
import com.example.docscanner.model.Document
import com.google.android.material.tabs.TabLayout

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
            openDocument(document)
        }
        
        binding.recyclerDocuments.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = documentsAdapter
        }

        // Setup FAB with animation
        binding.fabScan.apply {
            setOnClickListener { view ->
                view.startAnimation(AnimationUtils.loadAnimation(this@MainActivity, R.anim.button_press))
                if (checkCameraPermission()) {
                    startCameraActivity()
                } else {
                    requestPermissions()
                }
            }
        }

        // Setup TabLayout
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Filter documents based on selected tab
                when (tab?.position) {
                    0 -> filterDocuments("all")
                    1 -> filterDocuments("documents")
                    2 -> filterDocuments("photos")
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Set empty state visibility
        updateEmptyState(true)
    }

    private fun filterDocuments(filter: String) {
        // TODO: Implement document filtering
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.textEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerDocuments.visibility = if (isEmpty) View.GONE else View.VISIBLE
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

    private fun startCameraActivity() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
    }

    private fun openDocument(document: Document) {
        // TODO: Implement document opening
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
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions required to use the app", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // TODO: Refresh document list
    }
}
