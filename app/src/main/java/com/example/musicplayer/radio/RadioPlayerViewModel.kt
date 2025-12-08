package com.example.musicplayer.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.model.RadioStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RadioPlayerViewModel(private val repository: RadioRepository) : ViewModel() {

    private val _stations = MutableStateFlow<List<RadioStation>>(emptyList())
    val stations: StateFlow<List<RadioStation>> = _stations.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun search(query: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _stations.value = repository.searchStations(query)
            } finally {
                _loading.value = false
            }
        }
    }

    suspend fun topVoted(limit: Int = 50) : List<RadioStation> {
        return repository.topVoted(limit)
    }
}

