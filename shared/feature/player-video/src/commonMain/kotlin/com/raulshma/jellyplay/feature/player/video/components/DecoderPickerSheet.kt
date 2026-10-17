package com.raulshma.jellyplay.feature.player.video.components

import com.raulshma.jellyplay.feature.player.video.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_option_decoder_next
import com.raulshma.jellyplay.feature.player.video.generated.resources.player_video_decoder_mode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raulshma.jellyplay.core.designsystem.theme.ShapeCache
import com.raulshma.jellyplay.core.model.DecoderMode
import com.raulshma.jellyplay.core.ui.tv.ifElse
import com.raulshma.jellyplay.core.ui.tv.verticalWrapAround
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Cpu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecoderPickerSheet(
    currentMode: DecoderMode,
    onSelect: (DecoderMode) -> Unit,
    onDismiss: () -> Unit,
) {
    PickerSheetScaffold(
        title = stringResource(Res.string.player_video_decoder_mode),
        icon = Tabler.Outline.Cpu,
        onDismiss = onDismiss,
        preContent = {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(Res.string.player_option_decoder_next),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        },
    ) { isTv, focusRequester ->
        if (isTv) {
            LazyColumn(modifier = Modifier.verticalWrapAround()) {
                items(DecoderMode.entries, key = { it.name }) { mode ->
                    val isSelected = mode == currentMode
                    val isFirstOrSelected = isSelected || (currentMode !in DecoderMode.entries && mode == DecoderMode.entries.firstOrNull())
                    TvFocusableOptionRow(
                        selected = isSelected,
                        onClick = { onSelect(mode); onDismiss() },
                        focusRequesterModifier = Modifier.ifElse(isFirstOrSelected, Modifier.focusRequester(focusRequester)),
                        trailingCheck = true,
                    ) {
                        TvOptionRowLabel(text = mode.localizedPlayerLabel(), selected = isSelected)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DecoderMode.entries.forEach { mode ->
                    val isSelected = mode == currentMode
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onSelect(mode)
                            onDismiss()
                        },
                        label = {
                            Text(
                                mode.localizedPlayerLabel(),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = ShapeCache.smoothPill,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            enabled = true,
                            selected = isSelected,
                        ),
                    )
                }
            }
        }
    }
}
