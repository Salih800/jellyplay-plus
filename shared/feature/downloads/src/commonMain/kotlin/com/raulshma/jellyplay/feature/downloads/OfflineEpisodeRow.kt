package com.raulshma.jellyplay.feature.downloads

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Check
import com.raulshma.jellyplay.core.designsystem.theme.ShapeCache
import com.raulshma.jellyplay.core.model.OfflineMediaItem
import com.raulshma.jellyplay.core.model.DownloadStatus
import com.raulshma.jellyplay.core.model.formatBytes
import com.raulshma.jellyplay.core.model.toMediaItem
import com.raulshma.jellyplay.core.ui.components.LocalMediaQuickActionController
import com.raulshma.jellyplay.core.ui.components.episodeContextLine
import com.raulshma.jellyplay.core.ui.image.MediaImage
import com.raulshma.jellyplay.core.ui.tv.rememberTvFocusState
import com.raulshma.jellyplay.core.ui.tv.tvFocusIndicator
import com.raulshma.jellyplay.feature.downloads.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OfflineEpisodeRow(
    item: OfflineMediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    onToggleSelection: (() -> Unit)? = null,
) {
    val focus = rememberTvFocusState()
    val quickActions = LocalMediaQuickActionController.current
    val mediaItem = remember(item) { item.toMediaItem() }
    Surface(shape = ShapeCache.smooth12, color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
                .then(focus.focusModifier)
                .tvFocusIndicator(focus, ShapeCache.smooth12)
                .combinedClickable(
                    onClick = { if (selected != null) onToggleSelection?.invoke() else onClick() },
                    onLongClick = { quickActions?.show(mediaItem) },
                )
                .padding(12.dp),
        ) {
            if (selected != null) Checkbox(checked = selected, onCheckedChange = { onToggleSelection?.invoke() })
            MediaImage(
                url = item.posterPath.orEmpty(),
                fallbackUrls = listOfNotNull(item.backdropPath),
                contentDescription = null,
                modifier = Modifier.width(96.dp).aspectRatio(16f / 9f).clip(ShapeCache.smooth12),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                episodeContextLine(item.mediaType, null, item.seasonNumber, item.episodeNumber)?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                Text(item.totalSizeBytes.formatBytes(), style = MaterialTheme.typography.bodySmall)
                item.downloadStatus?.takeIf { it != DownloadStatus.COMPLETED }?.let {
                    val label = when (it) {
                        DownloadStatus.FAILED -> Res.string.downloads_status_failed
                        DownloadStatus.PAUSED -> Res.string.downloads_status_paused
                        DownloadStatus.CANCELLED -> Res.string.downloads_status_cancelled
                        DownloadStatus.DOWNLOADING -> Res.string.downloads_status_downloading
                        DownloadStatus.QUEUED -> Res.string.downloads_status_queued
                        else -> Res.string.downloads_status_waiting
                    }
                    Text(stringResource(label), style = MaterialTheme.typography.labelSmall)
                }
                if (item.downloadStatus == DownloadStatus.DOWNLOADING && item.totalSizeBytes > 0) {
                    LinearProgressIndicator(
                        progress = { (item.downloadedBytes.toFloat() / item.totalSizeBytes).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (!mediaItem.isPlayed && item.playedPercentage > 0) {
                    LinearProgressIndicator(
                        progress = { (item.playedPercentage.toFloat() / 100).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (mediaItem.isPlayed) {
                Icon(Tabler.Outline.Check, contentDescription = stringResource(Res.string.downloads_watched),
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
