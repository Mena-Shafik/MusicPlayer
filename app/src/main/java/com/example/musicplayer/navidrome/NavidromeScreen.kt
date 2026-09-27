package com.example.musicplayer.navidrome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.musicplayer.ui.components.background.AuroraRibbonBackground

private val Accent = Color(0xFFFFA500)
private val Connected5ad = Color(0xFF5AD07A)
private val Destructive = Color(0xFFFF7D6A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavidromeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: NavidromeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NavidromeViewModel(context = context) as T
            }
        }
    )
    val step by viewModel.step.collectAsState()

    // Form field state is lifted here (outside the ViewModel) so it survives a failed test and going back to the form -- no retyping the address/username.
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var allowSelfSigned by remember { mutableStateOf(false) }

    val title = when (step) {
        is NavidromeUiStep.Testing -> "Testing connection"
        is NavidromeUiStep.Form -> "Navidrome server"
        else -> "Streaming source"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuroraRibbonBackground()
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (step is NavidromeUiStep.Form) viewModel.showEmpty() else navController.popBackStack()
                        }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val s = step) {
                    is NavidromeUiStep.Empty -> EmptyState(onConnect = { viewModel.showForm() })
                    is NavidromeUiStep.Form -> FormState(
                        serverUrl = serverUrl,
                        onServerUrlChange = { serverUrl = it },
                        username = username,
                        onUsernameChange = { username = it },
                        password = password,
                        onPasswordChange = { password = it },
                        allowSelfSigned = allowSelfSigned,
                        onAllowSelfSignedChange = { allowSelfSigned = it },
                        onTest = { viewModel.testConnection(serverUrl, username, password, allowSelfSigned) },
                        onCancel = { viewModel.showEmpty() }
                    )
                    is NavidromeUiStep.Testing -> TestingState(
                        hostLabel = serverUrl,
                        state = s,
                        onTryAgain = { viewModel.showForm() }
                    )
                    is NavidromeUiStep.Connected -> ConnectedState(
                        state = s,
                        onSyncNow = { viewModel.syncNow() },
                        onDisconnect = { viewModel.disconnectServer() }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onConnect: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.CloudOff, contentDescription = null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(44.dp))
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(text = "No server connected", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Point the app at your own Navidrome server to stream everything you own — the local library keeps working exactly as it does now.",
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 13.5.sp,
            lineHeight = 20.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FeatureBullet(icon = Icons.Filled.CloudQueue, label = "Stream your whole collection")
            FeatureBullet(icon = Icons.Filled.Download, label = "Keep any album on device")
            FeatureBullet(icon = Icons.Filled.Sync, label = "Playlists sync both ways")
        }
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(text = "Connect Navidrome server", icon = Icons.Filled.Link, onClick = onConnect)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Any Subsonic-compatible server works — Navidrome, Airsonic, Gonic.",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 11.5.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(bottom = 22.dp)
        )
    }
}

@Composable
private fun FeatureBullet(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
        Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FormState(
    serverUrl: String,
    onServerUrlChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    allowSelfSigned: Boolean,
    onAllowSelfSignedChange: (Boolean) -> Unit,
    onTest: () -> Unit,
    onCancel: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val canTest = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NavidromeField(
                    label = "SERVER ADDRESS",
                    value = serverUrl,
                    onValueChange = onServerUrlChange,
                    placeholder = "https://music.example.com"
                )
                Text(
                    text = "Include the port if it isn't 443 — e.g. :4533",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.5.sp
                )
            }
            NavidromeField(label = "USERNAME", value = username, onValueChange = onUsernameChange, placeholder = "")
            NavidromeField(
                label = "PASSWORD",
                value = password,
                onValueChange = onPasswordChange,
                placeholder = "",
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color.White.copy(alpha = 0.55f)
                        )
                    }
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAllowSelfSignedChange(!allowSelfSigned) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Allow self-signed certificate", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(text = "Only for servers on your own network", color = Color.White.copy(alpha = 0.5f), fontSize = 11.5.sp)
                }
                Switch(checked = allowSelfSigned)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(text = "Test connection", enabled = canTest, onClick = onTest)
        Spacer(modifier = Modifier.height(10.dp))
        GhostButton(text = "Cancel", onClick = onCancel)
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavidromeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { if (placeholder.isNotEmpty()) Text(text = placeholder, color = Color.White.copy(alpha = 0.3f)) },
            singleLine = true,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.09f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.09f),
                focusedBorderColor = Accent,
                unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Accent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
    }
}

@Composable
private fun TestingState(hostLabel: String, state: NavidromeUiStep.Testing, onTryAgain: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(imageVector = Icons.Filled.Dns, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
            Column {
                Text(text = hostLabel, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(modifier = Modifier.height(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.steps.forEach { checklistStep -> ChecklistRow(checklistStep) }
        }
        if (state.localSongCount != null) {
            Spacer(modifier = Modifier.height(18.dp))
            InfoBanner(text = "Your ${state.localSongCount} local songs stay first. Server copies of the same track are hidden behind them.")
        }
        Spacer(modifier = Modifier.weight(1f))
        if (state.failed) {
            state.errorMessage?.let {
                Text(text = it, color = Destructive, fontSize = 12.5.sp, modifier = Modifier.padding(bottom = 10.dp))
            }
            PrimaryButton(text = "Try again", onClick = onTryAgain)
        } else {
            WaitingButton(text = "Finishing scan…")
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ChecklistRow(step: ChecklistStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            when (step.status) {
                ChecklistStepStatus.DONE -> Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = Accent)
                ChecklistStepStatus.FAILED -> Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = Destructive)
                ChecklistStepStatus.ACTIVE -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.2.dp,
                    color = Accent,
                    trackColor = Color.White.copy(alpha = 0.18f)
                )
                ChecklistStepStatus.PENDING -> Icon(
                    imageVector = Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        Column(
            modifier = Modifier.let { if (step.status == ChecklistStepStatus.PENDING) it.alpha(0.4f) else it }
        ) {
            Text(text = step.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (step.subtitle.isNotEmpty()) {
                Text(text = step.subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.5.sp)
            }
        }
    }
}

@Composable
private fun InfoBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Accent.copy(alpha = 0.10f))
            .border(1.dp, Accent.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Icon(imageVector = Icons.Filled.Info, contentDescription = null, tint = Accent, modifier = Modifier.size(19.dp))
        Text(text = text, color = Color.White.copy(alpha = 0.8f), fontSize = 12.5.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun ConnectedState(state: NavidromeUiStep.Connected, onSyncNow: () -> Unit, onDisconnect: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Dns, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(text = state.serverHost, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Connected5ad))
                        Text(
                            text = "Connected" + (state.serverVersion?.let { " · Navidrome $it" } ?: ""),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.songCount} songs · synced ${formatRelativeTime(state.lastSyncedAtMs)}",
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 12.5.sp
                )
                if (state.syncing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Accent)
                } else {
                    Text(
                        text = "Sync now",
                        color = Accent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onSyncNow)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        DestructiveButton(text = "Disconnect server", onClick = onDisconnect)
        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun formatRelativeTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return "never"
    val diffMs = System.currentTimeMillis() - timestampMs
    val minutes = diffMs / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> "${minutes / 60} h ago"
        else -> "${minutes / (60 * 24)} d ago"
    }
}

@Composable
private fun PrimaryButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Color.White else Color.White.copy(alpha = 0.3f))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF111111), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, color = Color(0xFF111111), fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WaitingButton(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White.copy(alpha = 0.6f), fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GhostButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White.copy(alpha = 0.62f), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DestructiveButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, Destructive.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Destructive, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// Small pill switch matching the one used on the Settings screen -- duplicated locally since the Settings one is private to that file.
@Composable
private fun Switch(checked: Boolean) {
    val trackColor = if (checked) Accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f)
    val borderColor = if (checked) Accent else Color.White.copy(alpha = 0.45f)
    val thumbColor = if (checked) Accent else Color.White.copy(alpha = 0.55f)
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(trackColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(13.dp))
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(thumbColor)
        )
    }
}

// Previews: one per screen state (matching the design's Setup 1-4 mockups) plus a stacked ChecklistRow status preview, calling the private state composables directly (bypassing NavidromeViewModel/DataStore/EncryptedSharedPreferences) to render standalone.

/** 360x780 matches the design system's phone-frame metric ("Frame 360x780, status bar 28"). */
@Composable
private fun PreviewFrame(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .width(360.dp)
            .height(780.dp)
    ) {
        AuroraRibbonBackground()
        content()
    }
}

@Preview(name = "Navidrome — 1 no server connected", showBackground = true, backgroundColor = 0xFF03050C)
@Composable
private fun NavidromeEmptyStatePreview() {
    PreviewFrame { EmptyState(onConnect = {}) }
}

@Preview(name = "Navidrome — 2 server details", showBackground = true, backgroundColor = 0xFF03050C)
@Composable
private fun NavidromeFormStatePreview() {
    PreviewFrame {
        FormState(
            serverUrl = "https://music.example.com",
            onServerUrlChange = {},
            username = "marla",
            onUsernameChange = {},
            password = "hunter2",
            onPasswordChange = {},
            allowSelfSigned = true,
            onAllowSelfSignedChange = {},
            onTest = {},
            onCancel = {}
        )
    }
}

@Preview(name = "Navidrome — 3a testing in progress", showBackground = true, backgroundColor = 0xFF03050C)
@Composable
private fun NavidromeTestingInProgressPreview() {
    PreviewFrame {
        TestingState(
            hostLabel = "music.example.com",
            state = NavidromeUiStep.Testing(
                steps = listOf(
                    ChecklistStep("Server reachable", "TLS valid · 84 ms", ChecklistStepStatus.DONE),
                    ChecklistStep("Signed in as marla", "Subsonic API 1.16.1", ChecklistStepStatus.DONE),
                    ChecklistStep("Reading library", "612 songs · 48 albums so far", ChecklistStepStatus.ACTIVE),
                    ChecklistStep("Matching your local files", "So nothing appears twice", ChecklistStepStatus.PENDING)
                )
            ),
            onTryAgain = {}
        )
    }
}

@Preview(name = "Navidrome — 3b testing failed", showBackground = true, backgroundColor = 0xFF03050C)
@Composable
private fun NavidromeTestingFailedPreview() {
    PreviewFrame {
        TestingState(
            hostLabel = "music.example.com",
            state = NavidromeUiStep.Testing(
                steps = listOf(
                    ChecklistStep("Server reachable", "TLS valid · 84 ms", ChecklistStepStatus.DONE),
                    ChecklistStep("Signed in as marla", "Subsonic API 1.16.1", ChecklistStepStatus.FAILED),
                    ChecklistStep("Reading library", "", ChecklistStepStatus.PENDING),
                    ChecklistStep("Matching your local files", "So nothing appears twice", ChecklistStepStatus.PENDING)
                ),
                failed = true,
                errorMessage = "Wrong username or password"
            ),
            onTryAgain = {}
        )
    }
}

@Preview(name = "Navidrome — 4 connected, sync settings", showBackground = true, backgroundColor = 0xFF03050C)
@Composable
private fun NavidromeConnectedStatePreview() {
    PreviewFrame {
        ConnectedState(
            state = NavidromeUiStep.Connected(
                serverHost = "music.example.com",
                serverVersion = "0.53.3",
                songCount = 2418,
                albumCount = 186,
                lastSyncedAtMs = System.currentTimeMillis() - 2 * 60_000
            ),
            onSyncNow = {},
            onDisconnect = {}
        )
    }
}

@Preview(name = "ChecklistRow — all statuses", showBackground = true, backgroundColor = 0xFF03050C)
@Composable
private fun ChecklistRowStatusesPreview() {
    Column(modifier = Modifier.padding(18.dp)) {
        ChecklistRow(ChecklistStep("Server reachable", "TLS valid · 84 ms", ChecklistStepStatus.DONE))
        ChecklistRow(ChecklistStep("Reading library", "612 songs · 48 albums so far", ChecklistStepStatus.ACTIVE))
        ChecklistRow(ChecklistStep("Matching your local files", "So nothing appears twice", ChecklistStepStatus.PENDING))
        ChecklistRow(ChecklistStep("Signed in as marla", "Wrong username or password", ChecklistStepStatus.FAILED))
    }
}
