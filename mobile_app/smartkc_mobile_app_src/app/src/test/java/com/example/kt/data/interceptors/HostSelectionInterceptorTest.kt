package com.example.kt.data.interceptors

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.kt.BuildConfig
import com.example.kt.utils.PreferenceKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class HostSelectionInterceptorTest {

    @Test
    fun storedUploadUrlOverridesPlaceholderAndTrimsTrailingSlash() {
        val captured = CapturingTerminalInterceptor()
        val client = testClient(
            preferences = mutablePreferencesOf(
                stringPreferencesKey(PreferenceKeys.UPLOAD_URL) to "https://upload.example.com/base/"
            ),
            terminalInterceptor = captured
        )

        client.newCall(
            Request.Builder()
                .url("http://__url__/api/smart_kc_uploader?retry=1")
                .header("X-Test", "kept")
                .post("payload".toRequestBody("text/plain".toMediaType()))
                .build()
        ).execute().close()

        val request = captured.request
        assertEquals("https://upload.example.com/base/api/smart_kc_uploader?retry=1", request.url.toString())
        assertEquals("POST", request.method)
        assertEquals("kept", request.header("X-Test"))
    }

    @Test
    fun missingUploadUrlPreferenceUsesBuildConfigDefault() {
        val captured = CapturingTerminalInterceptor()
        val client = testClient(
            preferences = emptyPreferences(),
            terminalInterceptor = captured
        )

        client.newCall(
            Request.Builder()
                .url("http://__url__/api/smart_kc_uploader")
                .build()
        ).execute().close()

        assertEquals(
            "${BuildConfig.UPLOAD_URL.trimEnd('/')}/api/smart_kc_uploader",
            captured.request.url.toString()
        )
    }

    private fun testClient(
        preferences: Preferences,
        terminalInterceptor: CapturingTerminalInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HostSelectionInterceptor(FakePreferencesDataStore(preferences)))
            .addInterceptor(terminalInterceptor)
            .build()
    }

    private class FakePreferencesDataStore(preferences: Preferences) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flowOf(preferences)

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            throw UnsupportedOperationException("updateData is not used by these tests")
        }
    }

    private class CapturingTerminalInterceptor : Interceptor {
        lateinit var request: Request

        override fun intercept(chain: Interceptor.Chain): Response {
            request = chain.request()
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("OK".toResponseBody("text/plain".toMediaType()))
                .build()
        }
    }
}
