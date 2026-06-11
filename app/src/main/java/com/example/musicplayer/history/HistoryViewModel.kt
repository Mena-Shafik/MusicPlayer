package com.example.musicplayer.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.model.Song
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(context: Context) : ViewModel() {
    private val repository = HistoryRepository(context)
    
    val history: StateFlow<List<Song>> = repository.history

    fun addToHistory(song: Song) {
        viewModelScope.launch {
            repository.addToHistory(song)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
