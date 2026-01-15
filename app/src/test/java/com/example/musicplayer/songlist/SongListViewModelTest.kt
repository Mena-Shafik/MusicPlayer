package com.example.musicplayer.songlist

import com.example.musicplayer.model.RadioStation
import com.example.musicplayer.model.Song
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import android.util.Log

@OptIn(ExperimentalCoroutinesApi::class)
class SongListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Mock Log.d to avoid "not mocked" errors
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun userStations_initializedCorrectly() = runTest(testDispatcher) {
        val defaultStations = listOf(
            RadioStation("z103", "Z103.5", "", "", "Canada", "", 128),
            RadioStation("virgin", "Virgin 99.9", "", "", "Canada", "", 128)
        )
        val vm = SongListViewModel(userStationsInitial = defaultStations)
        val stations = vm.userStations.first()
        assertEquals(2, stations.size)
        assertEquals("Z103.5", stations[0].name)
    }

    @Test
    fun toggleRadioSelected_flipsState() = runTest(testDispatcher) {
        val vm = SongListViewModel()
        val initialState = vm.isRadioSelected.first()
        assertFalse(initialState)

        vm.toggleRadioSelected()
        val newState = vm.isRadioSelected.first()
        assertTrue(newState)

        vm.toggleRadioSelected()
        val finalState = vm.isRadioSelected.first()
        assertFalse(finalState)
    }

    @Test
    fun query_storedCorrectly() = runTest(testDispatcher) {
        val vm = SongListViewModel()
        vm.setQuery("test")
        val query = vm.query.first()
        assertEquals("test", query)

        vm.clearQuery()
        val clearedQuery = vm.query.first()
        assertEquals("", clearedQuery)
    }

    @Test
    fun selectSongById_updatesSelection() = runTest(testDispatcher) {
        val songs = listOf(
            Song(1, "Hello", "A", 100.0, "p1"),
            Song(2, "World", "B", 100.0, "p2"),
        )
        val vm = SongListViewModel(initialSongs = songs)
        vm.selectSongById(2)
        val selectedId = vm.selectedId.first()
        assertEquals(2, selectedId)
    }

    @Test
    fun initialSongs_loaded() = runTest(testDispatcher) {
        val songs = listOf(
            Song(1, "Hello", "A", 100.0, "p1"),
            Song(2, "World", "B", 100.0, "p2"),
            Song(3, "Other", "C", 100.0, "p3"),
        )
        val vm = SongListViewModel(initialSongs = songs)
        val loadedSongs = vm.songs.first()
        assertEquals(3, loadedSongs.size)
        assertEquals("Hello", loadedSongs[0].title)
    }

    @Test
    fun sortOrder_initialized() = runTest(testDispatcher) {
        val vm = SongListViewModel()
        val sortOrder = vm.sortOrder.first()
        assertEquals(SongListViewModel.SortOrder.TITLE_ASC, sortOrder)
    }
}
