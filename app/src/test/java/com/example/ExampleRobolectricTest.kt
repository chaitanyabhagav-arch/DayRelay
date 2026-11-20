package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BridgeConfig
import com.example.data.model.BridgeProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DayRelay", appName)
    }

    @Test
    fun `bridge config formats ntfy subscription url cleanly`() {
        val config = BridgeConfig(
            providerType = BridgeProviderType.NTFY,
            ntfyBaseUrl = "https://ntfy.sh/",
            ntfyTopic = "/dayrelay-demo123"
        )
        assertTrue(config.isConfigured)
        assertEquals("https://ntfy.sh/dayrelay-demo123", config.ntfySubscriptionUrl)
    }
}
