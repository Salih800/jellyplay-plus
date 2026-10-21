package com.raulshma.jellyplay.navigation

import android.app.Application
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.raulshma.jellyplay.R
import com.raulshma.jellyplay.core.model.OfflineMode
import com.raulshma.jellyplay.navigation.components.OverflowMenuItems
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OverflowMenuLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun settingsRemainsReachableInShortWindows() {
        var settingsOpened = false
        val label = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.menu_settings)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.height(240.dp)) {
                    OverflowMenuItems(
                        onSurpriseClick = {}, onSyncPlayClick = {}, onDownloadsClick = {},
                        onToggleOffline = {}, onPlayOnClick = {}, onShortcutsClick = {},
                        onSettingsClick = { settingsOpened = true },
                        offlineMode = OfflineMode.ONLINE,
                        isGoingOnline = false,
                        alignToStart = true,
                    )
                }
            }
        }
        compose.onNodeWithText(label).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(settingsOpened) }
    }
}
