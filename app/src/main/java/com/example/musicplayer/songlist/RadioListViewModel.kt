package com.example.musicplayer.songlist

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Simple ViewModel that holds up to 4 image slots for the radio grid.
 * Each slot may be null (placeholder) or an ImageBitmap.
 * The ViewModel is intentionally lightweight and doesn't depend on Android Context.
 */
class RadioListViewModel(
    private val slotCount: Int = 4
) : ViewModel() {

    private val _images = MutableStateFlow<List<ImageBitmap?>>(List(slotCount) { null })
    val images: StateFlow<List<ImageBitmap?>> = _images

    /** Replace the entire list of images. The list will be normalized to [slotCount] items. */
    fun setImages(newImages: List<ImageBitmap?>) {
        val normalized = (newImages.take(slotCount) + List(maxOf(0, slotCount - newImages.size)) { null }).take(slotCount)
        _images.value = normalized
    }

    /** Reset all slots to placeholders (null). */
    fun clear() {
        _images.value = List(slotCount) { null }
    }

    /** Update a single slot (no-op if index out of range). */
    fun updateAt(index: Int, bitmap: ImageBitmap?) {
        if (index !in 0 until slotCount) return
        val copy = _images.value.toMutableList()
        copy[index] = bitmap
        _images.value = copy
    }
}