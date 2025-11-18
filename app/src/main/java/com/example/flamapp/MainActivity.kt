package com.example.flamapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.os.*
import android.util.Size
import android.util.Log
import android.view.TextureView
import android.opengl.GLSurfaceView
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.hardware.camera2.*
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private lateinit var glSurface: GLSurfaceView
    private lateinit var fpsText: TextView
    private lateinit var toggleButton: Button

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null

    private lateinit var glRenderer: GLRenderer
    private var isProcessed = true
    private var frameCount = 0
    private var lastTime = System.currentTimeMillis()

    companion object {
        private const val TAG = "Flamapp"
        private const val CAMERA_PERMISSION_CODE = 100
    }

    // Declare native method
    external fun processFrameNV21(input: ByteArray, width: Int, height: Int): ByteArray?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "=== onCreate ===")

        // Check if native libraries are loaded
        if (!FlamappApplication.isNativeLoaded()) {
            Log.e(TAG, "✗✗✗ Native libraries NOT loaded! ✗✗✗")
            Toast.makeText(
                this,
                "Native libraries failed to load. Check logcat.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Log.d(TAG, "✓ Native libraries confirmed loaded")
        }

        setContentView(R.layout.activity_main)

        // Initialize views
        textureView = findViewById(R.id.texture_view)
        glSurface = findViewById(R.id.gl_surface)
        fpsText = findViewById(R.id.fps_text)
        toggleButton = findViewById(R.id.toggle_button)

        // Setup toggle button
        toggleButton.setOnClickListener {
            isProcessed = !isProcessed
            if (isProcessed) {
                textureView.visibility = android.view.View.GONE
                glSurface.visibility = android.view.View.VISIBLE
                toggleButton.text = "Mode: Processed"
                Log.d(TAG, "Switched to PROCESSED mode")
            } else {
                textureView.visibility = android.view.View.VISIBLE
                glSurface.visibility = android.view.View.GONE
                toggleButton.text = "Mode: Raw"
                Log.d(TAG, "Switched to RAW mode")
            }
        }

        // Ensure we start in processed mode
        textureView.visibility = android.view.View.GONE
        glSurface.visibility = android.view.View.VISIBLE
        toggleButton.text = "Mode: Processed"

        // Setup OpenGL
        try {
            glSurface.setEGLContextClientVersion(2)
            glRenderer = GLRenderer(640, 480)
            glSurface.setRenderer(glRenderer)
            glSurface.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            Log.d(TAG, "✓ OpenGL setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "✗ OpenGL setup failed", e)
            Toast.makeText(this, "OpenGL initialization failed", Toast.LENGTH_SHORT).show()
        }

        // Setup camera thread
        startBackgroundThread()

        // Check and request permission
        if (hasCameraPermission()) {
            Log.d(TAG, "Permission already granted")
            Handler(Looper.getMainLooper()).postDelayed({
                setupCamera()
            }, 100)
        } else {
            Log.d(TAG, "Requesting camera permission")
            requestCameraPermission()
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.d(TAG, "=== onRequestPermissionsResult === requestCode: $requestCode")

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d(TAG, "✓ Permission GRANTED")

                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!isFinishing && !isDestroyed) {
                            Log.d(TAG, "Setting up camera after permission grant")
                            setupCamera()
                        }
                    }, 500)
                } else {
                    Log.e(TAG, "✗ Permission DENIED")
                    Toast.makeText(
                        this,
                        "Camera permission is required to use this app",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun startBackgroundThread() {
        cameraThread = HandlerThread("CameraThread").also { it.start() }
        cameraHandler = Handler(cameraThread!!.looper)
        Log.d(TAG, "✓ Background thread started")
    }

    private fun stopBackgroundThread() {
        cameraThread?.quitSafely()
        try {
            cameraThread?.join()
            cameraThread = null
            cameraHandler = null
            Log.d(TAG, "✓ Background thread stopped")
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    private fun setupCamera() {
        if (cameraDevice != null) {
            Log.d(TAG, "Camera already open")
            return
        }

        if (!hasCameraPermission()) {
            Log.e(TAG, "✗ No camera permission")
            return
        }

        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            val cameraId = manager.cameraIdList[0]
            Log.d(TAG, "Opening camera: $cameraId")

            // Setup ImageReader
            imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2)
            imageReader?.setOnImageAvailableListener({ reader ->
                handleImageAvailable(reader)
            }, cameraHandler)

            // Open camera
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "✗ No permission at open time")
                return
            }

            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d(TAG, "✓✓✓ Camera OPENED ✓✓✓")
                    cameraDevice = camera
                    startCameraPreview()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "Camera disconnected")
                    cameraDevice?.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "✗ Camera error: $error")
                    cameraDevice?.close()
                    cameraDevice = null
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Camera error occurred",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }, cameraHandler)

        } catch (e: CameraAccessException) {
            Log.e(TAG, "✗ CameraAccessException", e)
        } catch (e: Exception) {
            Log.e(TAG, "✗ Exception opening camera", e)
        }
    }

    private fun startCameraPreview() {
        val camera = cameraDevice ?: return
        val reader = imageReader ?: return

        try {
            val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(reader.surface)

            camera.createCaptureSession(
                listOf(reader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return

                        Log.d(TAG, "✓ Capture session configured")
                        captureSession = session

                        captureRequestBuilder.set(
                            CaptureRequest.CONTROL_MODE,
                            CameraMetadata.CONTROL_MODE_AUTO
                        )
                        captureRequestBuilder.set(
                            CaptureRequest.CONTROL_AF_MODE,
                            CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                        )

                        try {
                            session.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                cameraHandler
                            )
                            Log.d(TAG, "✓✓ Camera preview STARTED ✓✓")
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "✗ Failed to start preview", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "✗ Failed to configure camera")
                        Toast.makeText(
                            this@MainActivity,
                            "Camera configuration failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                cameraHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "✗ CameraAccessException in startPreview", e)
        }
    }

    private fun handleImageAvailable(reader: ImageReader) {
        var image: Image? = null
        try {
            image = reader.acquireLatestImage()
            if (image == null) {
                Log.w(TAG, "Acquired null image")
                return
            }

            val width = image.width
            val height = image.height

            // Convert to NV21
            val nv21 = yuv420ToNV21(image)
            Log.v(TAG, "Converted to NV21, size: ${nv21.size} bytes")

            // Check if native library is loaded
            if (!FlamappApplication.isNativeLoaded()) {
                Log.e(TAG, "✗✗✗ Native library not loaded, skipping processing ✗✗✗")
                return
            }

            // Process frame with OpenCV
            val start = System.nanoTime()
            val processed = try {
                processFrameNV21(nv21, width, height)
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "✗✗✗ Native method call failed ✗✗✗", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "✗✗✗ Error calling native method ✗✗✗", e)
                null
            }
            val end = System.nanoTime()

            if (processed == null) {
                Log.e(TAG, "✗✗✗ processFrameNV21 returned NULL ✗✗✗")
                return
            }

            Log.v(TAG, "✓ Processed frame size: ${processed.size} bytes")

            val processingTime = (end - start) / 1_000_000f

            // Update FPS
            frameCount++
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTime >= 1000) {
                val fps = frameCount.toFloat() / ((currentTime - lastTime) / 1000f)
                runOnUiThread {
                    fpsText.text = String.format("FPS: %.1f | Proc: %.1f ms", fps, processingTime)
                }
                frameCount = 0
                lastTime = currentTime
            }

            // Send to OpenGL for rendering
            if (processed.isNotEmpty()) {
                glSurface.queueEvent {
                    Log.v(TAG, "Updating GL texture")
                    glRenderer.updateFrame(processed)
                    glSurface.requestRender()
                }
            } else {
                Log.e(TAG, "✗ Processed frame is empty!")
            }

        } catch (e: Exception) {
            Log.e(TAG, "✗ Error processing frame", e)
            e.printStackTrace()
        } finally {
            image?.close()
        }
    }

    private fun yuv420ToNV21(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 2
        val nv21 = ByteArray(ySize + uvSize)

        val planes = image.planes
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride

        val uRowStride = uPlane.rowStride
        val uPixelStride = uPlane.pixelStride

        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride

        // Copy Y plane
        var pos = 0
        for (row in 0 until height) {
            val yRowStart = row * yRowStride
            if (yPixelStride == 1) {
                // Contiguous, fast path
                yBuffer.position(yRowStart)
                yBuffer.get(nv21, pos, width)
                pos += width
            } else {
                // Uncommon pixel stride for Y
                for (col in 0 until width) {
                    nv21[pos++] = yBuffer.get(yRowStart + col * yPixelStride)
                }
            }
        }

        // Copy interleaved VU (NV21 format: V then U)
        val chromaHeight = height / 2
        val chromaWidth = width / 2
        var uvPos = ySize
        for (row in 0 until chromaHeight) {
            val uRowStart = row * uRowStride
            val vRowStart = row * vRowStride
            for (col in 0 until chromaWidth) {
                val uIndex = uRowStart + col * uPixelStride
                val vIndex = vRowStart + col * vPixelStride
                nv21[uvPos++] = vBuffer.get(vIndex) // V first
                nv21[uvPos++] = uBuffer.get(uIndex) // U second
            }
        }

        return nv21
    }

    private fun closeCamera() {
        try {
            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            imageReader?.close()
            imageReader = null

            Log.d(TAG, "✓ Camera closed")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error closing camera", e)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "=== onResume ===")
        glSurface.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "=== onPause ===")
        glSurface.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "=== onDestroy ===")
        closeCamera()
        stopBackgroundThread()
    }
}