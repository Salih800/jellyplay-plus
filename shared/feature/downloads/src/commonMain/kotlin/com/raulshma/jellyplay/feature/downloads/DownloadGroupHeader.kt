package com.raulshma.jellyplay.feature.downloads

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.ChevronDown
import com.composables.icons.tabler.outline.ChevronRight
import com.raulshma.jellyplay.core.designsystem.theme.ShapeCache
import com.raulshma.jellyplay.core.model.formatBytes
import com.raulshma.jellyplay.core.ui.tv.rememberTvFocusState
import com.raulshma.jellyplay.core.ui.tv.tvFocusIndicator
import com.raulshma.jellyplay.feature.downloads.generated.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DownloadGroupHeader(
    title: String,
    count: Int,
    totalBytes: Long,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDetails: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    activeCount: Int = 0,
    serverTotal: Int? = null,
    selectionState: ToggleableState = ToggleableState.Off,
    onToggleSelection: (() -> Unit)? = null,
    selectionEnabled: Boolean = true,
) {
    val focus = rememberTvFocusState()
    val state = stringResource(if (expanded) Res.string.downloads_expanded else Res.string.downloads_collapsed)
    Surface(
        shape = ShapeCache.smooth12,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onToggleSelection != null) {
                val description = stringResource(Res.string.downloads_select_group, title)
                TriStateCheckbox(
                    state = selectionState,
                    onClick = onToggleSelection,
                    enabled = selectionEnabled,
                    modifier = Modifier.semantics { contentDescription = description },
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
                    .then(focus.focusModifier)
                    .tvFocusIndicator(focus, ShapeCache.smooth12)
                    .semantics { stateDescription = state }
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .padding(14.dp),
            ) {
                Icon(if (expanded) Tabler.Outline.ChevronDown else Tabler.Outline.ChevronRight, null)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        (if (serverTotal != null) stringResource(Res.string.downloads_episode_fraction, count, serverTotal)
                        else pluralStringResource(Res.plurals.downloads_episode_count, count, count)) +
                            " · " + totalBytes.formatBytes(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (activeCount > 0) {
                        Text(
                            pluralStringResource(Res.plurals.downloads_active_count, activeCount, activeCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (onDetails != null) {
                TextButton(onClick = onDetails) { Text(stringResource(Res.string.downloads_details)) }
            }
        }
    }
}

@Composable
internal fun downloadSeasonTitle(number: Int?, name: String?): String = when {
    number == 0 -> stringResource(Res.string.downloads_specials)
    number != null -> stringResource(Res.string.downloads_season_number, number)
    !name.isNullOrBlank() -> name
    else -> stringResource(Res.string.downloads_unknown_season)
}

@Composable
internal fun DownloadCategoryTabs(
    selected: OfflineLibraryFilter,
    onSelect: (OfflineLibraryFilter) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PrimaryScrollableTabRow(selectedTabIndex = selected.ordinal, modifier = modifier, edgePadding = 0.dp) {
        OfflineLibraryFilter.entries.forEach { category ->
            Tab(
                enabled = enabled,
                selected = category == selected,
                onClick = { onSelect(category) },
                text = { Text(stringResource(category.labelRes)) },
            )
        }
    }
}
