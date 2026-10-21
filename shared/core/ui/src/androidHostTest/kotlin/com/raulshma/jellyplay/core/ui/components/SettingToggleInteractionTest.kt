package com.raulshma.jellyplay.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.Clock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingToggleInteractionTest {
    @get:Rule val compose = createComposeRule()

    private fun showToggle() {
        compose.setContent {
            MaterialTheme {
                var checked by remember { mutableStateOf(false) }
                SettingToggleItem(
                    icon = Tabler.Outline.Clock,
                    title = "Show Clock",
                    subtitle = "Display the current time",
                    checked = checked,
                    onCheckedChange = { checked = it },
                )
            }
        }
    }

    @Test fun tappingTitleChangesValue() {
        showToggle()
        compose.onNodeWithText("Show Clock").performTouchInput { click() }
        compose.onNode(isToggleable()).assertIsOn()
    }

    @Test fun tappingSwitchChangesValue() {
        showToggle()
        compose.onNode(isToggleable()).performTouchInput { click() }
        compose.onNode(isToggleable()).assertIsOn()
    }

    @Test fun groupedRowsRemainClickableAfterRecomposition() {
        compose.setContent {
            MaterialTheme {
                var clock by remember { mutableStateOf(false) }
                var remaining by remember { mutableStateOf(false) }
                SettingsItemList(total = 2) {
                    Column {
                        SettingToggleItem(Tabler.Outline.Clock, "Show Clock", "Current time", clock, onCheckedChange = { clock = it })
                        SettingToggleItem(Tabler.Outline.Clock, "Show Time Remaining", "Remaining time", remaining, onCheckedChange = { remaining = it })
                    }
                }
            }
        }
        compose.onNodeWithText("Show Clock").performTouchInput { click() }
        compose.onAllNodes(isToggleable())[0].assertIsOn()
        compose.onNodeWithText("Show Time Remaining").performTouchInput { click() }
        compose.onAllNodes(isToggleable())[1].assertIsOn()
        compose.onNodeWithText("Show Clock").performTouchInput { click() }
        compose.onAllNodes(isToggleable())[0].assertIsOff()
    }
}
