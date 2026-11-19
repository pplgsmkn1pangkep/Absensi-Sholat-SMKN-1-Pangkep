package com.example.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Konfigurasi eksplisit Firebase untuk SIM SHOLAT (SMKN 1 Pangkajene dan Kepulauan).
 * Disimpan eksplisit di kode sumber menggunakan FirebaseOptions agar tetap berfungsi saat
 * di-build menjadi APK meskipun file google-services.json tidak terbawa.
 */
object FirebaseConfig {
    const val PROJECT_ID = "ruang-sholat-smkn-1-pangkep"
    const val PROJECT_NUMBER = "556344409557"
    const val APPLICATION_ID = "1:556344409557:android:d35a48ab389e7d0977115c"
    const val API_KEY = "AIzaSyDaJosseKvZ15Bx6krWc5ZQeTBUyc4P38s"
    const val STORAGE_BUCKET = "ruang-sholat-smkn-1-pangkep.firebasestorage.app"
    const val DATABASE_ID = "(default)"

    fun buildOptions(): FirebaseOptions {
        return FirebaseOptions.Builder()
            .setProjectId(PROJECT_ID)
            .setApplicationId(APPLICATION_ID)
            .setApiKey(API_KEY)
            .setStorageBucket(STORAGE_BUCKET)
            .setGcmSenderId(PROJECT_NUMBER)
            .build()
    }

    fun ensureInitialized(context: Context): FirebaseApp {
        val appContext = context.applicationContext ?: context
        val existingApps = FirebaseApp.getApps(appContext)
        val matchingDefault = existingApps.firstOrNull {
            it.name == FirebaseApp.DEFAULT_APP_NAME && it.options.projectId == PROJECT_ID
        }
        if (matchingDefault != null) return matchingDefault

        val matchingNamed = existingApps.firstOrNull { it.name == PROJECT_ID }
        if (matchingNamed != null) return matchingNamed

        val options = buildOptions()
        return if (existingApps.none { it.name == FirebaseApp.DEFAULT_APP_NAME }) {
            FirebaseApp.initializeApp(appContext, options) ?: FirebaseApp.getInstance()
        } else {
            FirebaseApp.initializeApp(appContext, options, PROJECT_ID)
        }
    }

    fun getFirestore(context: Context? = null): FirebaseFirestore {
        val app = if (context != null) {
            ensureInitialized(context)
        } else {
            val defaultApp = FirebaseApp.getInstance()
            if (defaultApp.options.projectId == PROJECT_ID) {
                defaultApp
            } else {
                ensureInitialized(defaultApp.applicationContext)
            }
        }
        return if (app.name == FirebaseApp.DEFAULT_APP_NAME) {
            FirebaseFirestore.getInstance()
        } else {
            FirebaseFirestore.getInstance(app)
        }
    }
}
