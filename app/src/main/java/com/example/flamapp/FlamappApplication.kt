package com.example.flamapp

import android.app.Application
import android.util.Log

class FlamappApplication : Application() {

    companion object {
        private const val TAG = "Flamapp"
        // Will be true when both libraries loaded successfully
        @JvmStatic
        var nativeLoaded: Boolean = false
            private set

        // Helper to query from anywhere
        @JvmStatic
        fun isNativeLoaded(): Boolean = nativeLoaded
    }

    override fun onCreate() {
        super.onCreate()

        try {
            // Load OpenCV first because libflamapp depends on it
            System.loadLibrary("opencv_java4")
            // Then load your native library
            System.loadLibrary("flamapp")
            nativeLoaded = true
            Log.d(TAG, "Native libraries loaded successfully (Application)")
        } catch (e: UnsatisfiedLinkError) {
            nativeLoaded = false
            Log.e(TAG, "Failed to load native libraries in Application", e)
        } catch (e: Exception) {
            nativeLoaded = false
            Log.e(TAG, "Unexpected error loading native libraries", e)
        }
    }
}
