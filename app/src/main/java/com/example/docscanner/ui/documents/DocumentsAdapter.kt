package com.example.docscanner.ui.documents

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.docscanner.R
import com.example.docscanner.databinding.ItemDocumentBinding
import com.example.docscanner.model.Document
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Locale

class DocumentsAdapter(
    private var documents: List<Document>,
    private val onDocumentClick: (Document) -> Unit
) : RecyclerView.Adapter<DocumentsAdapter.DocumentViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    inner class DocumentViewHolder(
        private val binding: ItemDocumentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(document: Document) {
            binding.apply {
                // Set document name
                textName.text = document.name

                // Set modified date
                textDate.text = "Modified: ${dateFormat.format(document.modifiedAt)}"

                // Load thumbnail
                document.thumbnailPath?.let { path ->
                    try {
                        val bitmap = BitmapFactory.decodeFile(path)
                        imageThumbnail.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        imageThumbnail.setImageResource(
                            if (document.isPDF()) {
                                R.drawable.ic_pdf
                            } else {
                                R.drawable.ic_image
                            }
                        )
                    }
                } ?: run {
                    imageThumbnail.setImageResource(
                        if (document.isPDF()) {
                            R.drawable.ic_pdf
                        } else {
                            R.drawable.ic_image
                        }
                    )
                }

                // Set up tags
                chipGroupTags.removeAllViews()
                document.tags.forEach { tag ->
                    val chip = Chip(root.context).apply {
                        text = tag
                        isCheckable = false
                        setChipBackgroundColorResource(R.color.colorPrimary)
                        setTextColor(root.context.getColor(android.R.color.white))
                    }
                    chipGroupTags.addView(chip)
                }

                // Set click listener
                root.setOnClickListener {
                    onDocumentClick(document)
                }

                // Set up more options menu
                buttonMore.setOnClickListener { view ->
                    showPopupMenu(view, document)
                }
            }
        }

        private fun showPopupMenu(view: View, document: Document) {
            PopupMenu(view.context, view).apply {
                inflate(R.menu.menu_document_item)
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_share -> {
                            // TODO: Implement share functionality
                            true
                        }
                        R.id.action_rename -> {
                            // TODO: Implement rename functionality
                            true
                        }
                        R.id.action_delete -> {
                            // TODO: Implement delete functionality
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val binding = ItemDocumentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DocumentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(documents[position])
    }

    override fun getItemCount() = documents.size

    fun updateDocuments(newDocuments: List<Document>) {
        documents = newDocuments
        notifyDataSetChanged()
    }
}
