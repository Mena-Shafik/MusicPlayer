package com.example.musicplayer.radio

import com.example.musicplayer.model.RadioStation

interface RadioRepository {
    suspend fun searchStations(query: String, limit: Int = 50): List<RadioStation>
    suspend fun topVoted(limit: Int = 50): List<RadioStation>
    suspend fun getStationById(id: String): RadioStation?
}

class RadioRepositoryImpl(private val api: RadioApiService) : RadioRepository {
    override suspend fun searchStations(query: String, limit: Int): List<RadioStation> {
        return api.searchStations(query, limit).map { st ->
            // store the station with formatted name
            st.copy(name = com.example.musicplayer.Util.formatStation(st))
        }
    }

    override suspend fun topVoted(limit: Int): List<RadioStation> {
        return api.topVoted(limit).map { st ->
            st.copy(name = com.example.musicplayer.Util.formatStation(st))
        }
    }

    override suspend fun getStationById(id: String): RadioStation? {
        val list = api.searchStations("", 100)
        val found = list.firstOrNull { it.stationuuid == id }
        return found?.copy(name = com.example.musicplayer.Util.formatStation(found))
    }
}
