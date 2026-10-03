package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.BrowserSettings
import com.example.data.model.SpeedDialItem
import com.example.ui.components.HomeSpeedDialView
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun home_speed_dial_screenshot() {
    val sampleItems = listOf(
        SpeedDialItem(1, "Google", "https://www.google.com", 0),
        SpeedDialItem(2, "Wikipedia", "https://www.wikipedia.org", 1),
        SpeedDialItem(3, "GitHub", "https://github.com", 2)
    )

    composeTestRule.setContent {
      MyApplicationTheme(isIncognito = false) {
        HomeSpeedDialView(
            settings = BrowserSettings(),
            speedDialItems = sampleItems,
            isIncognito = false,
            onSearch = {},
            onSelectEngine = {},
            onSpeedDialClick = {},
            onAddSpeedDialClick = {},
            onRemoveSpeedDial = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/home_speed_dial.png")
  }
}
