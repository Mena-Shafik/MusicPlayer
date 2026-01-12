package com.example.musicplayer.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.musicplayer.Util

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MusicControls(
    isPlaying: Boolean,
    replayEnabled: Boolean,
    shuffleEnabled: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onReplayToggle: () -> Unit,
    onShuffleToggle: (Boolean) -> Unit,
    accentColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            IconButton(
                onClick = onReplayToggle,
                modifier = Modifier
                    .size(45.dp, 45.dp)
                    .padding(5.dp, 0.dp, 5.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Repeat,
                    contentDescription = if (replayEnabled) "replay on" else "replay off",
                    modifier = Modifier.size(45.dp, 45.dp),
                    tint = if (replayEnabled) accentColor else Util.dim(false)
                )
            }

            IconButton(
                onClick = onPrev,
                modifier = Modifier
                    .size(65.dp, 65.dp)
                    .padding(0.dp, 0.dp, 10.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "previous button",
                    modifier = Modifier.size(55.dp, 55.dp),
                    tint = accentColor
                )
            }
            val morphDuration = 320
            val targetSize = if (isPlaying) 96.dp else 96.dp
            val animatedSize by animateDpAsState(
                targetValue = targetSize,
                animationSpec = tween(durationMillis = morphDuration)
            )

            Box(
                modifier = Modifier
                    .size(animatedSize)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            val spec = tween<Float>(durationMillis = morphDuration)
                            (fadeIn(animationSpec = spec) + scaleIn(
                                initialScale = 1.15f,
                                animationSpec = spec
                            )) togetherWith
                                    (fadeOut(animationSpec = spec) + scaleOut(
                                        targetScale = 1.15f,
                                        animationSpec = spec
                                    ))
                        },
                        contentAlignment = Alignment.Center
                    ) { playing ->
                        val iconModifier = Modifier
                            .fillMaxSize()
                            .padding(5.dp, 0.dp, 5.dp, 0.dp)

                        if (playing) {
                            Icon(
                                imageVector = Icons.Filled.PauseCircleFilled,
                                contentDescription = "Pause",
                                modifier = iconModifier,
                                tint = accentColor
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayCircleFilled,
                                contentDescription = "Play",
                                modifier = iconModifier,
                                tint = accentColor
                            )
                        }
                    }
                }
            }
            /*Icon(
                imageVector = if (isPlaying) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                contentDescription = "play/pause Button",
                modifier = Modifier
                    .size(84.dp)
                    .padding(5.dp)
                    .clickable { onPlayPause() },
                tint = accentColor
            )*/

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(65.dp, 65.dp)
                    .padding(10.dp, 0.dp, 0.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "next button",
                    modifier = Modifier.size(55.dp, 55.dp),
                    tint = accentColor
                )
            }

            IconButton(
                onClick = { onShuffleToggle(!shuffleEnabled) },
                modifier = Modifier
                    .size(45.dp, 45.dp)
                    .padding(5.dp, 0.dp, 5.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = if (shuffleEnabled) "shuffle on" else "shuffle off",
                    modifier = Modifier.size(45.dp, 45.dp),
                    tint = if (shuffleEnabled) accentColor else Util.dim(false)
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RadioControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevStation: () -> Unit,
    onNextStation: () -> Unit,
    accentColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Row containing the play/pause button
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            IconButton(
                onClick = onPrevStation,
                modifier = Modifier
                    .size(65.dp, 65.dp)
                    .padding(0.dp, 0.dp, 10.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "previous button",
                    modifier = Modifier.size(55.dp, 55.dp),
                    tint = accentColor
                )
            }

            val morphDuration = 320
            val targetSize = if (isPlaying) 96.dp else 96.dp
            val animatedSize by animateDpAsState(
                targetValue = targetSize,
                animationSpec = tween(durationMillis = morphDuration)
            )

            Box(
                modifier = Modifier
                    .size(animatedSize)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            val spec = tween<Float>(durationMillis = morphDuration)
                            (fadeIn(animationSpec = spec) + scaleIn(
                                initialScale = 1.15f,
                                animationSpec = spec
                            )) togetherWith
                                    (fadeOut(animationSpec = spec) + scaleOut(
                                        targetScale = 1.15f,
                                        animationSpec = spec
                                    ))
                        },
                        contentAlignment = Alignment.Center
                    ) { isPlaying ->
                        val iconModifier = Modifier
                            .fillMaxSize()
                            .padding(5.dp, 0.dp, 5.dp, 0.dp)

                        if (isPlaying) {
                            Icon(
                                imageVector = Icons.Filled.PauseCircleFilled,
                                contentDescription = "Pause",
                                modifier = iconModifier,
                                tint = accentColor
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayCircleFilled,
                                contentDescription = "Play",
                                modifier = iconModifier,
                                tint = accentColor
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onNextStation,
                modifier = Modifier
                    .size(65.dp, 65.dp)
                    .padding(10.dp, 0.dp, 0.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "next button",
                    modifier = Modifier.size(55.dp, 55.dp),
                    tint = accentColor
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "MusicControls Preview (shuffle off, replay off)", backgroundColor = 0xFF000000)
@Composable
fun MusicControlsPreview_Toggled() {
    MaterialTheme {
        MusicControls(
            isPlaying = true,
            replayEnabled = false,
            shuffleEnabled = false,
            onPlayPause = {},
            onNext = {},
            onPrev = {},
            onReplayToggle = { },
            onShuffleToggle = { _ -> }
        )
    }
}

@Preview(showBackground = true, name = "RadioControls Preview", backgroundColor = 0xFF000000)
@Composable
fun RadioControlsPreview() {
    MaterialTheme {
        RadioControls(
            isPlaying = true,
            onPlayPause = {},
            onPrevStation = {},
            onNextStation = {}
        )
    }
}
