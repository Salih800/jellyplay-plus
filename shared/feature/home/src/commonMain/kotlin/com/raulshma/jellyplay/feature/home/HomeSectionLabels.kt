package com.raulshma.jellyplay.feature.home

import androidx.compose.runtime.Composable
import com.raulshma.jellyplay.core.model.HomeSection
import com.raulshma.jellyplay.core.model.HomeSectionType
import com.raulshma.jellyplay.core.model.descriptor
import com.raulshma.jellyplay.feature.home.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Translate built-in headers when rendered, preserving user and server supplied titles. */
@Composable
internal fun HomeSection.localizedTitle(): String {
    val seed = seedItem
    if (type == HomeSectionType.RECOMMENDATIONS && seed != null) {
        return stringResource(Res.string.home_because_you_watched, seed.name)
    }
    if (type == HomeSectionType.LATEST_MEDIA) {
        val prefix = type.descriptor.titleFor("")
        return if (title.startsWith(prefix)) {
            stringResource(Res.string.home_latest_library, title.removePrefix(prefix))
        } else title
    }
    // Offline rows already carry localized titles. Custom titles must remain intact.
    if (title != type.displayName) return title
    val resource = when (type) {
        HomeSectionType.CONTINUE_WATCHING -> Res.string.home_continue_watching
        HomeSectionType.CONTINUE_READING -> Res.string.home_continue_reading
        HomeSectionType.NEXT_UP -> Res.string.home_next_up
        HomeSectionType.RECENTLY_ADDED -> Res.string.home_recently_added
        HomeSectionType.RECOMMENDATIONS -> Res.string.home_recommended_for_you
        HomeSectionType.FAVORITES -> Res.string.home_favorites
        HomeSectionType.LIVE_TV -> Res.string.home_live_tv
        HomeSectionType.DOWNLOADED -> Res.string.home_downloaded
        else -> return title
    }
    return stringResource(resource)
}
