package com.raulshma.jellyplay.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config
import com.raulshma.jellyplay.core.ui.settingssearch.ResolvedSettingsItem
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsListPickerSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun searchResultBadgesLeaveRoomForTitleAndDescription() {
        val item = ResolvedSettingsItem(
            item = SettingsSearchCatalog.items.first().copy(isAdvanced = true),
            title = "Preferred Subtitle Language",
            subtitle = "Select default language for media subtitles",
            category = "Language & Subtitles",
        )
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(360.dp)) {
                    SettingsSearchResultRow(item, 0, 1, "Adv", onClick = {})
                }
            }
        }
        val title = compose.onNodeWithText(item.title, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val subtitle = compose.onNodeWithText(item.subtitle, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val category = compose.onNodeWithText(item.category, useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(title.bottom - title.top <= 48.dp)
        assertTrue(category.top >= subtitle.bottom)
    }

    @Test
    fun searchInputRequestsKeyboardWhenFocused() {
        var shown = false
        val keyboard = object : SoftwareKeyboardController {
            override fun show() { shown = true }
            override fun hide() {}
        }
        compose.setContent {
            CompositionLocalProvider(LocalSoftwareKeyboardController provides keyboard) {
                MaterialTheme {
                    PickerSearchField(
                        query = "", onQueryChange = {}, placeholder = "Search options",
                        requestFocus = true,
                    )
                }
            }
        }
        compose.onNodeWithTag("settings-picker-search").assertIsFocused()
        compose.runOnIdle { assertTrue(shown) }
    }

    @Test
    @Config(qualifiers = "w800dp-h180dp-land")
    fun searchingByCodeSelectsOriginalItem() {
        var selected: String? = null
        compose.setContent {
            MaterialTheme {
                SettingsListPickerSheet(
                    title = "Preferred language",
                    items = (1..20).map { "code$it" },
                    label = { "Language ${it.removePrefix("code")}" },
                    isSelected = { it == "code1" },
                    onDismiss = {},
                    onSelect = { selected = it },
                )
            }
        }
        compose.onNodeWithContentDescription("Search options").performClick()
        compose.onNodeWithTag("settings-picker-search").assertIsFocused().assertIsDisplayed()
        compose.onNodeWithText("Search options").performTextInput("code12")
        compose.onNodeWithText("Language 12").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("code12", selected) }
    }
}
