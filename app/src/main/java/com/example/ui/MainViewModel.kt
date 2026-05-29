package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class ToastModel(
    val message: String,
    val type: String // "info", "success", "error"
)

class MainViewModel : ViewModel() {

    private val _lang = MutableStateFlow("en")
    val lang: StateFlow<String> = _lang.asStateFlow()

    private val _history = MutableStateFlow<List<Bitmap>>(emptyList())
    val history: StateFlow<List<Bitmap>> = _history.asStateFlow()

    private val _historyIndex = MutableStateFlow(-1)
    val historyIndex: StateFlow<Int> = _historyIndex.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _activeCategory = MutableStateFlow<String?>(null)
    val activeCategory: StateFlow<String?> = _activeCategory.asStateFlow()

    private val _toast = MutableStateFlow<ToastModel?>(null)
    val toast: StateFlow<ToastModel?> = _toast.asStateFlow()

    fun setLang(langCode: String) {
        _lang.value = langCode
    }

    fun showToast(message: String, type: String = "info") {
        _toast.value = ToastModel(message, type)
    }

    fun clearToast() {
        _toast.value = null
    }

    fun setInitialImage(bitmap: Bitmap) {
        _history.value = listOf(bitmap)
        _historyIndex.value = 0
        _activeCategory.value = null
    }

    fun handleUndo() {
        val index = _historyIndex.value
        if (index > 0) {
            _historyIndex.value = index - 1
        }
    }

    fun handleRedo() {
        val index = _historyIndex.value
        val list = _history.value
        if (index < list.size - 1) {
            _historyIndex.value = index + 1
        }
    }

    fun setActiveCategory(category: String?) {
        _activeCategory.value = category
    }

    fun applyAIFeature(promptText: String, processingMsg: String, successMsg: String, errorMsg: String, serverErrorMsg: String) {
        val currentIndex = _historyIndex.value
        val list = _history.value
        if (currentIndex < 0 || _isProcessing.value) return

        val currentBitmap = list[currentIndex]

        _isProcessing.value = true
        showToast(processingMsg, "info")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Read API Key from BuildConfig
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    withContext(Dispatchers.Main) {
                        showToast("API Key is missing. Please set your GEMINI_API_KEY in the Secrets panel inside AI Studio.", "error")
                        _isProcessing.value = false
                    }
                    return@launch
                }

                // Compress current bitmap to Base64 (using 85% JPEG format is perfect for upload)
                val base64Data = compressBitmapToBase64(currentBitmap)

                // Build Request Payload
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            role = "user",
                            parts = listOf(
                                Part(text = "Edit this image: $promptText. Ensure the person's core identity remains intact. Provide only the edited image."),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Data))
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        responseModalities = listOf("IMAGE")
                    )
                )

                // Fire Retrofit Request
                val response = RetrofitClient.service.editImage(apiKey, request)

                // Parse base64 image candidate response
                val inlinePart = response.candidates?.firstOrNull()?.content?.parts?.find { it.inlineData != null }
                val responseData = inlinePart?.inlineData?.data

                if (responseData != null) {
                    val decodedBytes = Base64.decode(responseData, Base64.DEFAULT)
                    val returnedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                    if (returnedBitmap != null) {
                        withContext(Dispatchers.Main) {
                            // Slice history up to current active index and append new entry
                            val updatedList = list.subList(0, currentIndex + 1).toMutableList()
                            updatedList.add(returnedBitmap)
                            _history.value = updatedList
                            _historyIndex.value = updatedList.size - 1
                            showToast(successMsg, "success")
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showToast(serverErrorMsg, "error")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showToast(serverErrorMsg, "error")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error applying AI feature", e)
                withContext(Dispatchers.Main) {
                    showToast("$errorMsg: ${e.localizedMessage ?: "Network error"}", "error")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                }
            }
        }
    }

    private fun compressBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
