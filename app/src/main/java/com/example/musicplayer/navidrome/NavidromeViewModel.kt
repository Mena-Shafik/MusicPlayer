package com.example.musicplayer.navidrome

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.preferences.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ChecklistStepStatus { PENDING, ACTIVE, DONE, FAILED }

data class ChecklistStep(
    val title: String,
    val subtitle: String,
    val status: ChecklistStepStatus
)

sealed class NavidromeUiStep {
    object Empty : NavidromeUiStep()
    object Form : NavidromeUiStep()
    data class Testing(
        val steps: List<ChecklistStep>,
        val localSongCount: Int? = null,
        val errorMessage: String? = null,
        val failed: Boolean = false
    ) : NavidromeUiStep()
    data class Connected(
        val serverHost: String,
        val serverVersion: String?,
        val songCount: Int,
        val albumCount: Int,
        val lastSyncedAtMs: Long,
        val syncing: Boolean = false
    ) : NavidromeUiStep()
}

// Drives the 4-screen Navidrome connection flow as one state machine (matching the design's "the same screen becomes X" note) rather than four separate destinations.
class NavidromeViewModel(private val context: Context) : ViewModel() {
    private val repository = NavidromeRepository(context)

    private val _step = MutableStateFlow<NavidromeUiStep>(NavidromeUiStep.Empty)
    val step: StateFlow<NavidromeUiStep> = _step.asStateFlow()

    init {
        viewModelScope.launch {
            val connected = PreferencesManager.getNavidromeConnectedFlow(context).first()
            val credentials = NavidromeCredentialsStore.get(context)
            if (connected && credentials != null) {
                _step.value = NavidromeUiStep.Connected(
                    serverHost = credentials.serverUrl,
                    serverVersion = PreferencesManager.getNavidromeServerVersionFlow(context).first(),
                    songCount = PreferencesManager.getNavidromeSongCountFlow(context).first(),
                    albumCount = PreferencesManager.getNavidromeAlbumCountFlow(context).first(),
                    lastSyncedAtMs = PreferencesManager.getNavidromeLastSyncedAtFlow(context).first()
                )
            }
        }
    }

    fun showForm() { _step.value = NavidromeUiStep.Form }
    fun showEmpty() { _step.value = NavidromeUiStep.Empty }

    fun testConnection(serverUrl: String, username: String, password: String, allowSelfSigned: Boolean) {
        val trimmedUrl = serverUrl.trim()
        val steps = listOf(
            ChecklistStep("Server reachable", "", ChecklistStepStatus.ACTIVE),
            ChecklistStep("Signed in as $username", "Subsonic API 1.16.1", ChecklistStepStatus.PENDING),
            ChecklistStep("Reading library", "", ChecklistStepStatus.PENDING),
            ChecklistStep("Matching your local files", "So nothing appears twice", ChecklistStepStatus.PENDING)
        )
        _step.value = NavidromeUiStep.Testing(steps)

        viewModelScope.launch {
            var current = steps

            fun publish(index: Int, status: ChecklistStepStatus, subtitle: String? = null, failed: Boolean = false, errorMessage: String? = null) {
                current = current.toMutableList().also { list ->
                    val existing = list[index]
                    list[index] = existing.copy(status = status, subtitle = subtitle ?: existing.subtitle)
                }
                _step.value = NavidromeUiStep.Testing(current, failed = failed, errorMessage = errorMessage)
            }

            val reachability = repository.checkReachability(trimmedUrl, allowSelfSigned)
            val tookMs = when (reachability) {
                is ReachabilityResult.Reachable -> reachability.tookMs
                is ReachabilityResult.Unreachable -> {
                    publish(0, ChecklistStepStatus.FAILED, failed = true, errorMessage = reachability.message)
                    return@launch
                }
            }
            publish(0, ChecklistStepStatus.DONE, subtitle = "TLS valid · ${tookMs} ms")
            publish(1, ChecklistStepStatus.ACTIVE)

            val auth = repository.authenticate(trimmedUrl, username, password, allowSelfSigned)
            val serverVersion = when (auth) {
                is AuthResult.Success -> auth.serverVersion
                is AuthResult.Failure -> {
                    publish(1, ChecklistStepStatus.FAILED, failed = true, errorMessage = auth.message)
                    return@launch
                }
            }
            publish(1, ChecklistStepStatus.DONE)
            publish(2, ChecklistStepStatus.ACTIVE, subtitle = "Starting…")

            val progress = try {
                repository.scanLibrary(trimmedUrl, username, password, allowSelfSigned) { p ->
                    publish(2, ChecklistStepStatus.ACTIVE, subtitle = "${p.songCount} songs · ${p.albumCount} albums so far")
                }
            } catch (e: Exception) {
                publish(2, ChecklistStepStatus.FAILED, failed = true, errorMessage = e.message ?: "Couldn't read the library")
                return@launch
            }
            publish(2, ChecklistStepStatus.DONE, subtitle = "${progress.songCount} songs · ${progress.albumCount} albums")
            publish(3, ChecklistStepStatus.ACTIVE)

            val localCount = repository.localSongCount()
            publish(3, ChecklistStepStatus.DONE)
            _step.value = NavidromeUiStep.Testing(current, localSongCount = localCount)

            NavidromeCredentialsStore.save(context, NavidromeCredentials(trimmedUrl, username, password))
            val syncedAt = System.currentTimeMillis()
            PreferencesManager.setNavidromeConnected(context, true)
            PreferencesManager.setNavidromeServerVersion(context, serverVersion)
            PreferencesManager.setNavidromeLibraryCounts(context, progress.songCount, progress.albumCount)
            PreferencesManager.setNavidromeLastSyncedAt(context, syncedAt)
            PreferencesManager.setNavidromeAllowSelfSigned(context, allowSelfSigned)

            _step.value = NavidromeUiStep.Connected(
                serverHost = trimmedUrl,
                serverVersion = serverVersion,
                songCount = progress.songCount,
                albumCount = progress.albumCount,
                lastSyncedAtMs = syncedAt
            )
        }
    }

    fun syncNow() {
        val connected = _step.value as? NavidromeUiStep.Connected ?: return
        val credentials = NavidromeCredentialsStore.get(context) ?: return
        _step.value = connected.copy(syncing = true)
        viewModelScope.launch {
            val allowSelfSigned = PreferencesManager.getNavidromeAllowSelfSignedFlow(context).first()
            try {
                val progress = repository.scanLibrary(
                    credentials.serverUrl, credentials.username, credentials.password, allowSelfSigned
                ) { /* no incremental UI during a background re-sync */ }
                val syncedAt = System.currentTimeMillis()
                PreferencesManager.setNavidromeLibraryCounts(context, progress.songCount, progress.albumCount)
                PreferencesManager.setNavidromeLastSyncedAt(context, syncedAt)
                _step.value = connected.copy(
                    songCount = progress.songCount,
                    albumCount = progress.albumCount,
                    lastSyncedAtMs = syncedAt,
                    syncing = false
                )
            } catch (e: Exception) {
                _step.value = connected.copy(syncing = false)
            }
        }
    }

    fun disconnectServer() {
        viewModelScope.launch {
            repository.disconnect()
            _step.value = NavidromeUiStep.Empty
        }
    }
}
