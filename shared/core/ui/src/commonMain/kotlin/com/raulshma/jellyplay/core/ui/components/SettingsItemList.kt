package com.raulshma.jellyplay.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

/**
 * Auto-indexing container for a vertical list of settings rows.
 *
 * **Why this is deep.** Every settings screen used to hand-roll the same
 * `var idx = 0; val total = run { var c = N; if (showAdvanced) c += M; … }`
 * stanza so each row could feed `expressiveListShape(index, count)` via
 * `SettingListItem(index = idx++, count = total)`. Forgetting `idx++` (or
 * the count arithmetic after adding a conditional row) was the recurring
 * bug. This module owns the index increment: rows compose inside it, each
 * gets the next index through [LocalSettingsItemIndex], and the caller
 * supplies the [total] (which they already maintain and which is a pure
 * function of their conditionals).
 *
 * Usage:
 * ```
 * SettingsItemList(total = total) {
 *     SettingListItem(title = "...", onClick = { ... }) // index auto-filled
 *     if (showAdvanced) SettingListItem(...)
 * }
 * ```
 *
 * Why not auto-derive [total] too? Counting rows reliably needs a probe
 * composition, and the probe either pays the cost of composing each `ListItem`
 * or requires every row to early-return — both worse than letting the caller
 * state the count they already compute. The bug surface this kills is the
 * `idx++` omission and the index/total drift between sibling rows; the count
 * itself is already a single source of truth at the call site.
 *
 * Not a `LazyColumn`. It lives inside a `SettingsGroup { … }` content lambda
 * or a `Column`, where rows compose eagerly.
 *
 * @param total total number of rows that will compose in [content]. Feeds
 *   [LocalSettingsItemCount] for `expressiveListShape(index, count)`.
 */
@Composable
fun SettingsItemList(
    total: Int,
    content: @Composable () -> Unit,
) {
    // A fresh pass assigns indices in composition order. The counter must not
    // be snapshot state: reading and incrementing it while composing rows
    // invalidates those same rows continuously and can interrupt touch input.
    val indexHolder = SettingsItemIndex()
    CompositionLocalProvider(
        LocalSettingsItemIndex provides indexHolder,
        LocalSettingsItemCount provides total,
    ) {
        content()
    }
}

/**
 * Pass-local index holder supplied by [SettingsItemList]. Each row remembers
 * one slot from it, so local recomposition keeps the assigned index. A fresh
 * containing pass assigns all slots again, including conditional rows.
 * Outside a list, rows use their explicit index parameter.
 */
val LocalSettingsItemIndex = compositionLocalOf<SettingsItemIndex?> { null }

class SettingsItemIndex internal constructor() {
    private var next = 0
    internal fun takeNext(): Int = next++
}

/** Child-only recomposition retains its slot until the containing pass changes. */
@Composable
internal fun rememberSettingsItemIndex(fallback: Int): Int {
    val holder = LocalSettingsItemIndex.current
    return remember(holder, fallback) { holder?.takeNext() ?: fallback }
}

/** Total row count, supplied to [SettingsItemList] by the caller. */
val LocalSettingsItemCount = compositionLocalOf { 1 }

/**
 * Advances the [LocalSettingsItemIndex] counter by one, for rows that are part
 * of the [SettingsItemList] count but do not render a [SettingListItem] /
 * [SettingToggleItem] (which bump the counter themselves). Call this inside a
 * picker or custom row that occupies a slot, so subsequent list items get the
 * correct index for `expressiveListShape(index, count)`.
 *
 * No-op outside a [SettingsItemList].
 */
@Composable
fun ConsumeSettingsItemIndex() {
    rememberSettingsItemIndex(0)
}
