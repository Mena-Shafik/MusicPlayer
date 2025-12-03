package com.example.musicplayer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    // Call this to hide the splash screen when loading is complete
    fun setLoadingComplete() {
        _isLoading.value = false
    }
}