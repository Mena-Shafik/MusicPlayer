package com.example.musicplayer.data

import kotlinx.coroutines.flow.Flow

class SongRepository(private val songDao: SongDao, private val playlistDao: PlaylistDao) {
    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    fun getSongsByAlbum(albumId: Long): Flow<List<Song>> = songDao.getSongsByAlbumOrdered(albumId)

    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongWithPosition>> = playlistDao.getSongsForPlaylistOrdered(playlistId)

    suspend fun saveSong(song: Song) = songDao.insertSong(song)

    suspend fun createPlaylist(name: String, isTemporary: Boolean = false): Long {
        val p = Playlist(name = name, isTemporary = isTemporary)
        return playlistDao.insertPlaylist(p)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long, position: Int) {
        playlistDao.insertEntry(PlaylistEntry(playlistId = playlistId, songId = songId, position = position))
    }
}

