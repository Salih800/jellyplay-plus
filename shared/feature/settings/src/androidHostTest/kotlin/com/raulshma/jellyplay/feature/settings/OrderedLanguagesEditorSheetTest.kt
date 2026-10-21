package com.raulshma.jellyplay.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OrderedLanguagesEditorSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test
    @Config(qualifiers = "w800dp-h300dp-land")
    fun inlineSearchAddsToExistingOrderWithoutOpeningAnotherSheet() {
        var saved = listOf("eng")
        compose.setContent {
            MaterialTheme {
                var ordered by remember { mutableStateOf(saved) }
                OrderedLanguagesEditorSheet(
                    title = "Preferred subtitle languages", ordered = ordered,
                    onDismiss = {}, onChange = { ordered = it; saved = it },
                )
            }
        }
        compose.onNodeWithTag("settings-picker-search").assertIsDisplayed().performClick()
        compose.onNodeWithTag("settings-picker-search").assertIsFocused().performTextInput("tur")
        compose.onNodeWithText("Turkish").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(listOf("eng", "tur"), saved) }
        compose.onNodeWithText("1 · English").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("2 · Turkish").performScrollTo().assertIsDisplayed()
    }
}
