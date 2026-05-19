package com.example.kt.utils

import android.content.Intent
import android.os.Build
import java.io.Serializable

inline fun <reified T : Serializable> Intent.serializableExtraCompat(name: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getSerializableExtra(name) as? T
    }
}
