package com.raulshma.jellyplay.core.ui.navigation

import androidx.compose.runtime.Composable
import com.raulshma.jellyplay.core.ui.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Resolve display text at composition time; persisted navigation keys stay unchanged. */
@Composable
fun Route.localizedNavLabel(rail: Boolean = false): String = stringResource(
    when (this) {
        Route.Home -> Res.string.core_nav_home
        Route.Library -> Res.string.core_nav_library
        Route.Search -> Res.string.core_search
        Route.LiveTv -> Res.string.core_media_live_tv
        Route.MusicBrowse -> if (rail) Res.string.core_media_music_plural else Res.string.core_nav_browse
        Route.Shortcuts -> Res.string.core_nav_shortcuts
        Route.Downloads -> Res.string.core_nav_downloads
        Route.Newsletter -> Res.string.core_nav_newsletter
        Route.WatchProgressHeatmap -> Res.string.core_nav_insights
        Route.SyncPlay -> Res.string.core_nav_syncplay
        Route.Requests -> Res.string.core_nav_requests
        Route.UpcomingCalendar -> Res.string.core_nav_calendar
        Route.ArrQueue -> Res.string.core_nav_arr_queue
        Route.Settings -> Res.string.core_nav_settings
        Route.AdminDashboard -> Res.string.core_nav_admin
        else -> error("No navigation label for $this")
    },
)
