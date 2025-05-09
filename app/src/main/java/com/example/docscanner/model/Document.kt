package com.example.docscanner.model

import java.util.Date

data class Document(
    val id: Long = 0,
    var name: String,
    val path: String,
    val createdAt: Date = Date(),
    var modifiedAt: Date = Date(),
    var thumbnailPath: String? = null,
    var pdfPath: String? = null,
    var tags: List<String> = emptyList()
) {
    fun getFileExtension(): String {
        return path.substringAfterLast(".", "")
    }
    
    fun isImage(): Boolean {
        val extension = getFileExtension().lowercase()
        return extension in listOf("jpg", "jpeg", "png")
    }
    
    fun isPDF(): Boolean {
        return getFileExtension().lowercase() == "pdf"
    }
}
