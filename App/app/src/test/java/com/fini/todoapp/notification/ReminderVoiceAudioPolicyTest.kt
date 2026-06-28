package com.fini.todoapp.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderVoiceAudioPolicyTest {

    @Test
    fun automotiveVoiceUsesNavigationGuidanceAudio() {
        assertEquals(
            ReminderVoiceUsage.NavigationGuidance,
            ReminderVoiceAudioPolicy.usageFor(isAutomotive = true)
        )
    }

    @Test
    fun automotiveVoiceCanFallbackToMediaAudio() {
        assertEquals(
            listOf(ReminderVoiceUsage.NavigationGuidance, ReminderVoiceUsage.Assistant, ReminderVoiceUsage.Media),
            ReminderVoiceAudioPolicy.usagesFor(isAutomotive = true)
        )
    }

    @Test
    fun phoneVoiceKeepsMediaAudio() {
        assertEquals(
            ReminderVoiceUsage.Media,
            ReminderVoiceAudioPolicy.usageFor(isAutomotive = false)
        )
        assertEquals(
            listOf(ReminderVoiceUsage.Media),
            ReminderVoiceAudioPolicy.usagesFor(isAutomotive = false)
        )
    }

    @Test
    fun largeLandscapePhoneIsNotAutomotiveForVoice() {
        assertFalse(
            ReminderDevicePolicy.isAutomotiveDevice(
                hasAutomotiveFeature = false,
                uiModeType = 1,
                device = "emu64xa",
                model = "sdk_gphone64_x86_64",
                fingerprint = "google/sdk_gphone64_x86_64/phone",
                screenWidthDp = 900,
                screenHeightDp = 500
            )
        )
    }

    @Test
    fun actualCarDeviceIsAutomotiveForVoice() {
        assertTrue(
            ReminderDevicePolicy.isAutomotiveDevice(
                hasAutomotiveFeature = false,
                uiModeType = 1,
                device = "emulator_car64_x86_64",
                model = "automotive_distant_display_on_x86_64_emulator",
                fingerprint = "google/sdk_gcar_dd_x86_64/car",
                screenWidthDp = 900,
                screenHeightDp = 500
            )
        )
    }

    @Test
    fun voiceSelectionPrefersOfflineVietnameseVoice() {
        val selected = ReminderVoiceAudioPolicy.selectVoice(
            candidates = listOf(
                ReminderVoiceCandidate(
                    name = "en-us-offline",
                    language = "en",
                    country = "US",
                    isDefault = true,
                    requiresNetwork = false
                ),
                ReminderVoiceCandidate(
                    name = "vi-vn-offline",
                    language = "vi",
                    country = "VN",
                    isDefault = false,
                    requiresNetwork = false
                )
            ),
            defaultLanguage = "en",
            defaultCountry = "US"
        )

        assertEquals("vi-vn-offline", selected?.name)
    }

    @Test
    fun voiceSelectionFallsBackToOfflineDefaultWhenVietnameseNeedsNetwork() {
        val selected = ReminderVoiceAudioPolicy.selectVoice(
            candidates = listOf(
                ReminderVoiceCandidate(
                    name = "vi-vn-network",
                    language = "vi",
                    country = "VN",
                    isDefault = false,
                    requiresNetwork = true
                ),
                ReminderVoiceCandidate(
                    name = "en-us-offline",
                    language = "en",
                    country = "US",
                    isDefault = true,
                    requiresNetwork = false
                )
            ),
            defaultLanguage = "en",
            defaultCountry = "US"
        )

        assertEquals("en-us-offline", selected?.name)
    }
}
