package com.example.musicplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import com.example.musicplayer.model.Song
import java.io.File
import java.util.ArrayList
import java.util.Locale
import kotlin.toString

class Util {

    companion object {
        private const val TAG = "Util"

        fun getAllAudioFromDevice(context: Context): List<Song> {
            val tempAudioList: MutableList<Song> = ArrayList()
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            //String path = "/storage/emulated/0/Music/";
            Environment.getExternalStorageDirectory().toString() + "/Music/*"
            // what data to grab
            val projection = arrayOf(
                MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.ArtistColumns.ARTIST,
                MediaStore.Audio.AlbumColumns.ALBUM,
                MediaStore.Audio.AudioColumns.DURATION
            )
            // check if it is a song
            val where = MediaStore.Audio.Media.IS_MUSIC + "=1"
            val c = context.contentResolver.query(uri, projection, where, null, "title")
            var count = 0
            if (c != null) {
                while (c.moveToNext()) {
                    val tempPath = c.getString(0)
                    val path = tempPath.toUri()
                    // Skip entries without a valid path to avoid passing empty strings to MediaMetadataRetriever
                    if (path.toString().isBlank()) continue
                    val title = c.getString(1) ?: "Unknown"
                    val artist = c.getString(2) ?: "Unknown"
                    val album = c.getString(3)?: "Other"
                    val duration = c.getDouble(4)
                    //val song = Song(count, title, artist, path, duration, getAlbumArt(c.getString(0)))
                    val song = Song(count, album, title, artist, duration, path.toString() )
                    tempAudioList.add(song)
                    count++
                    val msg = "Album id: ${song.id} | Title: ${song.title} | Artist: ${song.artist} | Path: ${song.path} | Duration: ${Util.converter(song.duration)}"
                    Log.i("data", formatSongRow(song))
                }
                c.close()
            }
            return tempAudioList
        }

        private fun padOrTruncate(s: String?, width: Int): String {
            val str = s ?: "Unknown"
            return if (str.length <= width) str.padEnd(width) else str.take(width - 3) + "..."
        }

        fun formatSongTableHeader(): String {
            // %-4s = left-aligned width 4, %-30s = left-aligned width 30, etc.
            return String.format(Locale.US, "%-4s %-30s %-20s %-40s %8s",
                "ID", "Album","Title", "Artist", "Path", "Duration")
        }

        fun formatSongRow(song: Song): String {
            val id = song.id.toString()
            val album = padOrTruncate(song.album, 40)
            val title = padOrTruncate(song.title, 40)
            val artist = padOrTruncate(song.artist, 40)
            val duration = padOrTruncate(song.duration.toString(), 10)
            val path = padOrTruncate(song.path, 70)
            return String.format(Locale.US, "%-4s %-30s %-30s %-20s %8s %-40s",id, album, title, artist, duration, path)
        }

        fun converter(time: Double): String {
            var elapsedTime: String?
            val minutes = (time / 1000 / 60).toInt()
            val seconds = (time / 1000 % 60).toInt()
            elapsedTime = "$minutes:"
            if (seconds < 10) elapsedTime += "0"
            elapsedTime += seconds
            return elapsedTime
        }

        fun getAlbumArt(context: Context, uri: String?): ImageBitmap? {
            if (uri.isNullOrBlank()) return null

            val retriever = MediaMetadataRetriever()
            return try {
                // Parse the URI and decide whether to call setDataSource with a Uri (content://) or a file path
                val parsedUri = try { Uri.parse(uri) } catch (_: Exception) { null }
                val hasScheme = parsedUri?.scheme?.isNotBlank() == true

                if (hasScheme) {
                    // Common case: content:// uri from MediaStore
                    try {
                        retriever.setDataSource(context, parsedUri)
                    } catch (e: Exception) {
                        Log.w(TAG, "getAlbumArt: setDataSource(context, uri) failed for parsedUri=$parsedUri", e)
                        return null
                    }
                } else {
                    // Treat as a file path. Ensure the file exists before calling setDataSource.
                    val file = File(uri)
                    if (file.exists()) {
                        try {
                            retriever.setDataSource(file.absolutePath)
                        } catch (e: Exception) {
                            Log.w(TAG, "getAlbumArt: setDataSource(file) failed for file=${file.absolutePath}", e)
                            return null
                        }
                    } else {
                        // Path doesn't exist on disk; avoid calling setDataSource with a non-existent path
                        Log.w(TAG, "getAlbumArt: file does not exist: $uri")
                        return null
                    }
                }

                val art = retriever.embeddedPicture
                if (art == null || art.isEmpty()) {
                    null
                } else {
                    val bmp = BitmapFactory.decodeByteArray(art, 0, art.size)
                    bmp?.asImageBitmap()
                }
            } catch (e: Exception) {
                Log.w(TAG, "getAlbumArt failed for uri=$uri", e)
                null
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }

        /**
         * Return an Android Bitmap of the embedded album art for the provided uri/path, or null.
         * This mirrors the logic in getAlbumArt but returns Bitmap to be used for Palette and caching.
         */
        fun getAlbumArtBitmap(context: Context, uri: String?): Bitmap? {
            if (uri.isNullOrBlank()) return null

            val retriever = MediaMetadataRetriever()
            return try {
                val parsedUri = try { Uri.parse(uri) } catch (_: Exception) { null }
                val hasScheme = parsedUri?.scheme?.isNotBlank() == true

                if (hasScheme) {
                    try {
                        retriever.setDataSource(context, parsedUri)
                    } catch (e: Exception) {
                        Log.w(TAG, "getAlbumArtBitmap: setDataSource(context, uri) failed for parsedUri=$parsedUri", e)
                        return null
                    }
                } else {
                    val file = File(uri)
                    if (file.exists()) {
                        try {
                            retriever.setDataSource(file.absolutePath)
                        } catch (e: Exception) {
                            Log.w(TAG, "getAlbumArtBitmap: setDataSource(file) failed for file=${file.absolutePath}", e)
                            return null
                        }
                    } else {
                        Log.w(TAG, "getAlbumArtBitmap: file does not exist: $uri")
                        return null
                    }
                }

                val art = retriever.embeddedPicture
                if (art == null || art.isEmpty()) {
                    null
                } else {
                    BitmapFactory.decodeByteArray(art, 0, art.size)
                }
            } catch (e: Exception) {
                Log.w(TAG, "getAlbumArtBitmap failed for uri=$uri", e)
                null
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }



        /*fun converter(time: Float): Float {
            var elapsedTime: String
            val minutes = (time / 1000 / 60).toInt()
            val seconds = (time / 1000 % 60).toInt()
            elapsedTime = "$minutes:"
            if (seconds < 10) elapsedTime += "0"
            elapsedTime += seconds
            return elapsedTime.toFloat()
        }*/

        fun converter(time: Int): String {
            var elapsedTime: String?
            val minutes = (time / 1000 / 60)
            val seconds = (time / 1000 % 60)
            elapsedTime = "$minutes:"
            if (seconds < 10) elapsedTime += "0"
            elapsedTime += seconds
            return elapsedTime
        }

        fun darkerColor(color: Color, factor: Float = 0.5f): Color {
            return Color(
                red = (color.red * factor).coerceIn(0f, 1f),
                green = (color.green * factor).coerceIn(0f, 1f),
                blue = (color.blue * factor).coerceIn(0f, 1f),
                alpha = color.alpha
            )
        }

        fun dim(clicked: Boolean): Color{
            return if (clicked) Color.White else Color.White.copy(alpha = 0.4f)
        }


        fun getThumbnail(context: Context, uri: Uri?): Bitmap {
            val mmr = MediaMetadataRetriever()
            val bfo = BitmapFactory.Options()
            if (uri == null) {
                return BitmapFactory.decodeResource(context.resources, R.drawable.ic_vinyl_record)
            }
            return try {
                mmr.setDataSource(context, uri)
                val rawArt = mmr.embeddedPicture
                if (rawArt != null && rawArt.isNotEmpty()) {
                    BitmapFactory.decodeByteArray(rawArt, 0, rawArt.size, bfo)
                } else {
                    BitmapFactory.decodeResource(context.resources, R.drawable.ic_vinyl_record)
                }
            } catch (e: Exception) {
                Log.w(TAG, "getThumbnail: setDataSource failed for uri=$uri", e)
                BitmapFactory.decodeResource(context.resources, R.drawable.ic_vinyl_record)
            } finally {
                try { mmr.release() } catch (_: Exception) {}
            }
        }

        fun shortToast(context: Context, text:String){
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }

        //fun showSnack(view: View, text:String){
        //    Snackbar.make(view,text,Snackbar.LENGTH_LONG).show()
        //}
    }

    fun convertImageByteArrayToBitmap(imageData: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
    }

}

//// OUTDATED CODE???


/*private fun getPlayList(rootPath: String): ArrayList<Song>? {
    val fileList = ArrayList<Song>()
    return try {
        val rootFolder = File(rootPath)
        val files =
            rootFolder.listFiles() //here you will get NPE if directory doesn't contains  any file,handle it like this.
        for (file in files) {
            if (file.isDirectory) {
                if (getPlayList(file.absolutePath) != null) {
                    fileList.addAll(getPlayList(file.absolutePath)!!)
                } else {
                    break
                }
            } else if (file.name.endsWith(".mp3")) {
                var song: Song
                song = Song(file.name, file.absolutePath)
                fileList.add(song)
            }
        }
        fileList
    } catch (e: Exception) {
        null
    }
}*/


/*companion object {
    fun getAllAudioFromDevice(context: Context): List<Song> {
        val tempAudioList: MutableList<Song> = ArrayList()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.AudioColumns.DATA,
            MediaStore.Audio.AudioColumns.TITLE,
            MediaStore.Audio.ArtistColumns.ARTIST,
            MediaStore.Audio.AudioColumns.DURATION
        )
        val c = context.contentResolver.query(uri, projection, null, null, null)
        if (c != null) {
            while (c.moveToNext()) {

                val path = c.getString(0)
                val title = c.getString(1)
                val artist = c.getString(2)
                val duration = c.getDouble(3)
                val name = path.substring(path.lastIndexOf("/") + 1)
                val song = Song(
                    title,
                    artist,
                    path,
                    duration,
                    cover = getAlbumArt(c.getString(0))
                )
                //Log.e("Name :$name", " Album :$title")
                //Log.e("Path :$path", " Artist :$artist")
                tempAudioList.add(song)
                Log.d("data: ", "title: " + song.title + "Artist: " + song.artist + " duration: " + song.duration+ " path: " + song.paths)
            }
            c.close()
        }
        return tempAudioList
    }

}*/
