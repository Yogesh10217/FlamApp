package com.example.flamapp

import android.app.Application
import android.util.Log

class FlamappApplication : Application() {

    companion object {
        private const val TAG = "FlamappApp"

        @JvmStatic
        var nativeLoaded: Boolean = false
            private set

        @JvmStatic
        fun isNativeLoaded(): Boolean = nativeLoaded
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate")

        try {
            // Load OpenCV first (flamapp depends on it)
            System.loadLibrary("opencv_java4")
            Log.d(TAG, "✓ opencv_java4 loaded")

            // Then load your native library
            System.loadLibrary("flamapp")
            Log.d(TAG, "✓ flamapp loaded")

            nativeLoaded = true
            Log.d(TAG, "✓✓✓ All native libraries loaded successfully ✓✓✓")

        } catch (e: UnsatisfiedLinkError) {
            nativeLoaded = false
            Log.e(TAG, "✗✗✗ Failed to load native libraries ✗✗✗", e)
            Log.e(TAG, "Error: ${e.message}")
        } catch (e: Exception) {
            nativeLoaded = false
            Log.e(TAG, "✗✗✗ Unexpected error loading libraries ✗✗✗", e)
        }
    }
}