package com.raulshma.jellyplay.feature.settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Clock
import com.raulshma.jellyplay.core.ui.components.SettingToggleItem
import com.raulshma.jellyplay.core.ui.components.SettingsItemList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h873dp-440dpi")
class SettingsGroupInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lastRowOfLongGroupAcceptsTouchAfterScrolling() {
        compose.setContent {
            MaterialTheme {
                LazyColumn {
                    item {
                        SettingsGroup(Tabler.Outline.Clock, "Player", initiallyExpanded = true) {
                            SettingsItemList(total = 40) {
                                repeat(40) { index ->
                                    var checked by remember { mutableStateOf(false) }
                                    SettingToggleItem(Tabler.Outline.Clock, "Setting $index", "Description", checked,
                                        onCheckedChange = { checked = it })
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Setting 38").performScrollTo().performTouchInput { click() }
        compose.onNode(isToggleable() and hasAnyAncestor(hasText("Setting 38"))).assertIsOn()
    }
}
