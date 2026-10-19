package com.raulshma.jellyplay.feature.player.video.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.raulshma.jellyplay.core.designsystem.theme.ShapeCache
import com.raulshma.jellyplay.core.model.StreamingQuality
import com.raulshma.jellyplay.core.ui.tv.ifElse
import com.raulshma.jellyplay.core.ui.tv.verticalWrapAround
import com.raulshma.jellyplay.feature.player.video.generated.resources.Res
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_video_adaptive_bitrate_cap
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_video_adaptive_bitrate_off
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_video_adaptive_bitrate_on
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_video_quality_description
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_video_quality_title
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Video

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QualityPickerSheet(
    currentQuality: StreamingQuality,
    adaptiveBitrateEnabled: Boolean,
    onToggleAdaptiveBitrate: (Boolean) -> Unit,
    onSelect: (StreamingQuality) -> Unit,
    onDismiss: () -> Unit,
) {
    PickerSheetScaffold(
        title = stringResource(Res.string.player_video_quality_title),
        icon = Tabler.Outline.Video,
        subtitle = stringResource(Res.string.player_video_quality_description),
        onDismiss = onDismiss,
        scrollable = true,
    ) { isTv, focusRequester ->
        QualityPickerOptions(currentQuality, adaptiveBitrateEnabled, onToggleAdaptiveBitrate, onSelect, isTv, focusRequester)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QualityPickerOptions(
    currentQuality: StreamingQuality,
    adaptiveBitrateEnabled: Boolean,
    onToggleAdaptiveBitrate: (Boolean) -> Unit,
    onSelect: (StreamingQuality) -> Unit,
    isTv: Boolean,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
) {
    if (isTv) {
        Column {
            StreamingQuality.entries.forEach { quality ->
                TvFocusableOptionRow(
                    selected = quality == currentQuality,
                    enabled = enabled,
                    focusRequesterModifier = if (focusRequester != null && quality == currentQuality) Modifier.focusRequester(focusRequester) else Modifier,
                    onClick = { onSelect(quality) },
                    trailingCheck = true,
                ) { TvOptionRowLabel(quality.displayName, quality == currentQuality) }
            }
        }
    } else {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StreamingQuality.entries.forEach { quality ->
                FilterChip(
                    selected = quality == currentQuality,
                    enabled = enabled,
                    onClick = { onSelect(quality) },
                    label = { Text(quality.displayName) },
                    shape = ShapeCache.smoothPill,
                )
            }
        }
    }
    if (enabled && currentQuality == StreamingQuality.AUTO) {
        AdaptiveBitrateToggle(adaptiveBitrateEnabled, onToggleAdaptiveBitrate)
    }
}

@Composable
private fun AdaptiveBitrateToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(ShapeCache.smooth8)
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = stringResource(Res.string.player_video_adaptive_bitrate_cap),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (enabled) {
                    stringResource(Res.string.player_video_adaptive_bitrate_on)
                } else {
                    stringResource(Res.string.player_video_adaptive_bitrate_off)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
        )
    }
}
