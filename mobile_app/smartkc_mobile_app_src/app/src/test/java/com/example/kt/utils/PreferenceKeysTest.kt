package com.example.kt.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenceKeysTest {

    @Test
    fun datastorePreferenceKeysStayStable() {
        assertEquals("center_cutoff", PreferenceKeys.CENTER_CUTOFF)
        assertEquals("upload_url", PreferenceKeys.UPLOAD_URL)
        assertEquals("upload_secret", PreferenceKeys.UPLOAD_SECRET)
        assertEquals("upload_enabled", PreferenceKeys.UPLOAD_ENABLED)
        assertEquals("chosen_camera", PreferenceKeys.CHOSEN_CAMERA)
    }
}
