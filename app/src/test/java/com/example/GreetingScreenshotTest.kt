package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.Track
import com.example.ui.MiniPlayer
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
  fun greeting_screenshot() {
    val testTrack = Track(
        id = "amaz_grace_test",
        title = "Amazing Grace",
        artist = "Grace Choral Ensemble",
        audioUrl = "https://archive.org/download/hymns_instrumental_01/amazing_grace.mp3",
        durationSeconds = 236,
        category = "Hymns"
    )

    composeTestRule.setContent { 
      MyApplicationTheme { 
        Box(modifier = Modifier.background(Color.Black).padding(16.dp)) {
          MiniPlayer(
              track = testTrack,
              isPlaying = true,
              onPlayPauseToggle = {},
              onSkipNext = {},
              onClick = {},
              lowBatteryMode = false
          )
        }
      } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
