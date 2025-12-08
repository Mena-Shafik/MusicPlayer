package com.example.musicplayer.radio

import com.example.musicplayer.model.RadioStation

interface RadioRepository {
    suspend fun searchStations(query: String, limit: Int = 50): List<RadioStation>
    suspend fun topVoted(limit: Int = 50): List<RadioStation>
    suspend fun getStationById(id: String): RadioStation?
}

class RadioRepositoryImpl(private val api: RadioApiService) : RadioRepository {
    override suspend fun searchStations(query: String, limit: Int): List<RadioStation> {
        return api.searchStations(query, limit)
    }

    override suspend fun topVoted(limit: Int): List<RadioStation> {
        return api.topVoted(limit)
    }

    override suspend fun getStationById(id: String): RadioStation? {
        val list = api.searchStations("", 100)
        return list.firstOrNull { it.stationuuid == id }
    }
}
