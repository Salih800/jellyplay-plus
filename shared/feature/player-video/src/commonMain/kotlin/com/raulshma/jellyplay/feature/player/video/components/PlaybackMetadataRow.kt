package com.raulshma.jellyplay.feature.player.video.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Clock
import com.composables.icons.tabler.outline.Settings
import com.raulshma.jellyplay.core.ui.player.PlayerIconButton
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_video_playback_mode
import com.composables.icons.tabler.outline.Subtitles
import com.composables.icons.tabler.outline.Wifi
import com.raulshma.jellyplay.core.designsystem.theme.HdrColors
import com.raulshma.jellyplay.core.designsystem.theme.ShapeCache
import com.raulshma.jellyplay.core.designsystem.theme.playerOnScrim
import com.raulshma.jellyplay.core.designsystem.theme.playerScrimColor
import com.raulshma.jellyplay.core.model.MediaStream
import com.raulshma.jellyplay.core.model.StreamType
import com.raulshma.jellyplay.core.ui.tv.LocalTvMode
import com.raulshma.jellyplay.core.ui.tv.rememberTvFocusState
import com.raulshma.jellyplay.core.ui.tv.tvFocusIndicator
import com.raulshma.jellyplay.feature.player.video.TrackOption
import com.raulshma.jellyplay.feature.player.video.engine.PlaybackMetadataSnapshot
import com.raulshma.jellyplay.feature.player.video.generated.resources.Res
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_video_mpv_config_chip
import org.jetbrains.compose.resources.stringResource

/**
 * The resolution / codec / HDR / audio / channels chip row shown
 * above the seekbar in [PlayerControls] (e.g. pill chips for "4K",
 * "HEVC", "Dolby Vision", "TrueHD Atmos", "7.1").
 *
 * Each metadata item is rendered as a refined glassmorphic pill chip with a
 * category-tuned translucent backdrop, delicate hairline border, and crisp
 * typography. The subtitle delay chip triggers the subtitle delay overlay.
 *
 * When [mpvConfigNoticeActive] (the engine's custom-mpv-config subtitle
 * ownership snapshot reports user-owned `sub-*` keys), a warning-tinted chip
 * joins the row so the interaction is visible without opening the subtitle
 * sheet — the sheet's style tab carries the full case-by-case notice, and the
 * chip is the shortcut there.
 *
 * Pure function of its arguments: all state is hoisted to the caller.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlaybackMetadataRow(
    playMethod: String,
    hdrType: String?,
    mediaStreams: List<MediaStream>,
    videoStats: PlaybackMetadataSnapshot,
    audioTracks: List<TrackOption>,
    isConnectionMetered: Boolean = false,
    subtitleDelayMs: Long = 0L,
    onSubtitleDelayClick: () -> Unit = {},
    onPlaybackOptionsClick: (() -> Unit)? = null,
    mpvConfigNoticeActive: Boolean = false,
    onMpvConfigNoticeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // Server streams describe the original file. They cannot describe the
    // codecs, resolution or HDR of a newly transcoded stream.
    val isTranscoding = playMethod.contains("transcod", ignoreCase = true)
    val sourceStreams = if (isTranscoding) emptyList() else mediaStreams
    val videoStream = sourceStreams.firstOrNull { it.type == StreamType.VIDEO }
    val resolutionLabel = deriveResolutionLabel(videoStream, videoStats.videoResolution)
    val videoCodec = formatVideoCodec(videoStats.videoCodec ?: videoStream?.codec)

    // Some engines do not expose HDR classification. Source HDR remains a
    // valid fallback for direct playback, but never for a transcoded stream.
    val sourceHdr = if (isTranscoding) null else when {
        !videoStream?.videoDoViTitle.isNullOrBlank() -> "Dolby Vision"
        else -> videoStream?.videoRangeType ?: hdrType ?: videoStream?.videoRange
    }
    val hdrLabel = formatPlaybackHdr(videoStats.videoHdrType ?: sourceHdr)
    val isDolbyVision = hdrLabel == "Dolby Vision"

    val activeAudioTrack = audioTracks.find { it.isSelected }
    val audioCodecFromStats = videoStats.audioCodec
    val audioStream = sourceStreams.firstOrNull { it.type == StreamType.AUDIO }
    val rawAudioCodec = formatAudioCodec(audioCodecFromStats ?: audioStream?.codec)

    val canUseSourceAudio = !isTranscoding && (audioCodecFromStats == null ||
        formatAudioCodec(audioCodecFromStats) == formatAudioCodec(audioStream?.codec))
    val matchText = buildString {
        append(audioCodecFromStats.orEmpty())
        if (canUseSourceAudio) {
            append(" ${activeAudioTrack?.label.orEmpty()} ${audioStream?.title.orEmpty()} ${audioStream?.displayTitle.orEmpty()}")
        }
    }.lowercase()
    val isAtmos = matchText.contains("atmos")
    val isDtsX = matchText.contains("dts:x") || matchText.contains("dtsx")

    val audioLabel = buildString {
        if (rawAudioCodec != null) {
            append(rawAudioCodec)
            if (isAtmos && !rawAudioCodec.contains("ATMOS", ignoreCase = true)) append(" Atmos")
            else if (isDtsX && !rawAudioCodec.contains("X", ignoreCase = true)) append(" DTS:X")
        } else if (isAtmos) {
            append("Dolby Atmos")
        } else if (isDtsX) {
            append("DTS:X")
        }
    }

    val channels = videoStats.audioChannels ?: audioStream?.channels
    val channelsLabel = channels?.let { ch ->
        when (ch) {
            1 -> "Mono"
            2 -> "Stereo"
            6 -> "5.1"
            8 -> "7.1"
            else -> "${ch}ch"
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val warningColor = MaterialTheme.colorScheme.tertiary
    val hdrGold = HdrColors.hdr10Gold
    val dolbyVisionGold = HdrColors.dolbyVisionGold
    val onScrim = playerOnScrim()
    val defaultBackground = playerScrimColor().copy(alpha = 0.55f)
    val defaultBorder = onScrim.copy(alpha = 0.16f)

    val mpvConfigChipLabel = stringResource(Res.string.player_video_mpv_config_chip)

    val items = remember(
        resolutionLabel,
        videoCodec,
        hdrLabel,
        audioLabel,
        channelsLabel,
        isConnectionMetered,
        subtitleDelayMs,
        onSubtitleDelayClick,
        mpvConfigNoticeActive,
        mpvConfigChipLabel,
        onMpvConfigNoticeClick,
    ) {
        listOfNotNull(
            resolutionLabel?.let {
                MetadataItem(
                    text = it,
                    textColor = onScrim,
                    containerColor = defaultBackground,
                    borderColor = defaultBorder,
                )
            },
            videoCodec?.let {
                MetadataItem(
                    text = it,
                    textColor = onScrim,
                    containerColor = defaultBackground,
                    borderColor = defaultBorder,
                )
            },
            hdrLabel?.let {
                if (isDolbyVision) {
                    MetadataItem(
                        text = it,
                        textColor = dolbyVisionGold,
                        containerColor = dolbyVisionGold.copy(alpha = 0.12f),
                        borderColor = hdrGold.copy(alpha = 0.50f),
                    )
                } else {
                    MetadataItem(
                        text = it,
                        textColor = hdrGold,
                        containerColor = hdrGold.copy(alpha = 0.12f),
                        borderColor = hdrGold.copy(alpha = 0.40f),
                    )
                }
            },
            if (audioLabel.isNotBlank()) {
                if (isAtmos || isDtsX) {
                    MetadataItem(
                        text = audioLabel,
                        textColor = dolbyVisionGold,
                        containerColor = dolbyVisionGold.copy(alpha = 0.10f),
                        borderColor = hdrGold.copy(alpha = 0.38f),
                    )
                } else {
                    MetadataItem(
                        text = audioLabel,
                        textColor = onScrim.copy(alpha = 0.95f),
                        containerColor = defaultBackground,
                        borderColor = defaultBorder,
                    )
                }
            } else null,
            channelsLabel?.let {
                MetadataItem(
                    text = it,
                    textColor = onScrim.copy(alpha = 0.90f),
                    containerColor = defaultBackground,
                    borderColor = defaultBorder,
                )
            },
            if (subtitleDelayMs != 0L) {
                MetadataItem(
                    text = "Sub Delay ${formatDelayLabel(subtitleDelayMs)}",
                    textColor = primaryColor,
                    containerColor = primaryColor.copy(alpha = 0.16f),
                    borderColor = primaryColor.copy(alpha = 0.40f),
                    icon = Tabler.Outline.Clock,
                    onClick = onSubtitleDelayClick,
                )
            } else null,
            // Metered link explains a silent quality cap (AUTO caps at
            // MAX_BITRATE_METERED on cellular/metered Wi-Fi). Rendered as a
            // warning badge so the user understands why high-bitrate media transcodes.
            if (isConnectionMetered) {
                MetadataItem(
                    text = "Metered",
                    textColor = warningColor,
                    containerColor = warningColor.copy(alpha = 0.14f),
                    borderColor = warningColor.copy(alpha = 0.35f),
                    icon = Tabler.Outline.Wifi,
                )
            } else null,
            // Custom mpv config owns sub-* styling keys this session: in-app
            // subtitle-style controls for those keys are no-ops. The chip
            // surfaces that here — where the playback mode already lives —
            // and clicks through to the subtitle hub, whose style tab spells
            // out the full case-by-case notice (owned keys, the ASS force
            // requirement, the mpv.conf quoting trap).
            if (mpvConfigNoticeActive) {
                MetadataItem(
                    text = mpvConfigChipLabel,
                    textColor = warningColor,
                    containerColor = warningColor.copy(alpha = 0.14f),
                    borderColor = warningColor.copy(alpha = 0.35f),
                    icon = Tabler.Outline.Subtitles,
                    onClick = onMpvConfigNoticeClick,
                )
            } else null,
        )
    }

    if (items.isEmpty() && onPlaybackOptionsClick == null) return

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (onPlaybackOptionsClick != null) {
            PlayerIconButton(
                icon = Tabler.Outline.Settings,
                contentDescription = stringResource(Res.string.player_video_playback_mode),
                onClick = onPlaybackOptionsClick,
                modifier = Modifier.size(32.dp),
            )
        }
        items.forEach { item ->
            MetadataChip(
                item = item,
                onClick = item.onClick,
            )
        }
    }
}

@Composable
private fun MetadataChip(
    item: MetadataItem,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isTv = LocalTvMode.current
    val tvFocusState = rememberTvFocusState(focusedScale = 1.05f)

    Box(
        modifier = modifier
            .clip(ShapeCache.smoothPill)
            .background(item.containerColor)
            .border(1.dp, item.borderColor, ShapeCache.smoothPill)
            .then(
                if (onClick != null) {
                    Modifier
                        .then(if (isTv) tvFocusState.focusModifier else Modifier)
                        .then(if (isTv) Modifier.tvFocusIndicator(tvFocusState, ShapeCache.smoothPill) else Modifier)
                        .clickable(role = Role.Button, onClick = onClick)
                        .semantics { role = Role.Button }
                } else Modifier
            )
            .padding(horizontal = 11.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.5.dp),
        ) {
            if (item.hasIndicatorDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(item.dotColor),
                )
            }
            if (item.icon != null) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.textColor,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = item.text,
                color = item.textColor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontSize = 12.sp,
                ),
            )
        }
    }
}

private fun deriveResolutionLabel(
    videoStream: MediaStream?,
    videoResolutionFromStats: String?,
): String? {
    if (!videoResolutionFromStats.isNullOrBlank()) {
        val parts = videoResolutionFromStats.lowercase().split("x")
        if (parts.size == 2) {
            val statWidth = parts[0].trim().toIntOrNull()
            val statHeight = parts[1].trim().toIntOrNull()
            if (statHeight != null || statWidth != null) {
                return resolutionLabelFor(statWidth, statHeight)
            }
        }
    }
    return resolutionLabelFor(videoStream?.width, videoStream?.height)
}

private fun resolutionLabelFor(width: Int?, height: Int?): String? = when {
    (width != null && width >= 3800) || (height != null && height >= 2000) -> "4K"
    (width != null && width >= 2500) || (height != null && height >= 1400) -> "1440p"
    (width != null && width >= 1800) || (height != null && height >= 900) -> "1080p"
    (width != null && width >= 1200) || (height != null && height >= 700) -> "720p"
    (width != null && width >= 700) || (height != null && height >= 460) -> "480p"
    height != null && height > 0 -> "${height}p"
    else -> null
}

internal fun formatVideoCodec(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val upper = raw.substringBefore('(').trim().removePrefix("video/").uppercase()
    return when {
        upper == "H264" || upper == "H.264" || upper == "AVC" -> "H.264"
        upper == "H265" || upper == "H.265" || upper == "HEVC" -> "HEVC"
        upper == "AV1" || upper == "AV01" -> "AV1"
        upper == "VP9" -> "VP9"
        upper == "VP8" -> "VP8"
        upper.contains("MPEG4") || upper.contains("MP4V") -> "MPEG-4"
        upper.contains("MPEG2") -> "MPEG-2"
        else -> upper
    }
}

internal fun formatAudioCodec(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    // Decoder descriptions may describe a whole codec family rather than
    // identify the selected bitstream. Never guess AC-3 vs E-AC-3 from A/52B.
    if (raw.trim().startsWith("ATSC A/52B", ignoreCase = true)) return "AC-3 / E-AC-3"
    val upper = raw.substringBefore('(').trim().removePrefix("audio/").uppercase()
    return when {
        upper == "EAC3" || upper == "E-AC3" || upper == "EC-3" -> "E-AC-3"
        upper == "AC3" || upper == "AC-3" -> "AC-3"
        upper == "TRUEHD" -> "TrueHD"
        upper == "DTSHD" || upper == "DTS-HD" -> "DTS-HD"
        upper == "DTSHD_MA" || upper == "DTS-HD MA" -> "DTS-HD MA"
        upper == "DCA" || upper == "DTS" -> "DTS"
        upper == "FLAC" -> "FLAC"
        upper == "AAC" || upper == "MP4A-LATM" || upper == "ADVANCED AUDIO CODING" || upper == "ADVANCED AUDIO CODEC" -> "AAC"
        upper == "OPUS" -> "Opus"
        upper == "VORBIS" -> "Vorbis"
        upper == "MP3" -> "MP3"
        upper == "ALAC" -> "ALAC"
        upper == "PCM" || upper.contains("PCM") -> "PCM"
        else -> upper
    }
}

internal fun formatPlaybackHdr(raw: String?): String? {
    val normalized = raw?.lowercase()?.replace(" ", "")?.replace("_", "") ?: return null
    return when {
        normalized.isBlank() || normalized == "sdr" -> null
        normalized.contains("dolbyvision") || normalized.contains("dovi") -> "Dolby Vision"
        normalized in listOf("hdr10plus", "hdr10+") -> "HDR10+"
        normalized == "hdr10" -> "HDR10"
        normalized == "hlg" -> "HLG"
        normalized == "hdr" -> "HDR"
        else -> raw.uppercase()
    }
}

internal fun compactPlaybackEngineName(name: String?): String? = when (name?.lowercase()) {
    null, "" -> null
    "exoplayer" -> "Exo"
    "mpv" -> "MPV"
    "libvlc" -> "VLC"
    else -> name
}

private data class MetadataItem(
    val text: String,
    val textColor: Color,
    val containerColor: Color,
    val borderColor: Color,
    val icon: ImageVector? = null,
    val hasIndicatorDot: Boolean = false,
    val dotColor: Color = Color.Unspecified,
    val onClick: (() -> Unit)? = null,
)

