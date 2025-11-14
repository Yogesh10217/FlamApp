package com.example.flamapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.media.Image
import android.media.ImageReader
import android.os.*
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.opengl.GLSurfaceView
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.hardware.camera2.*

class MainActivity : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private lateinit var glSurface: GLSurfaceView
    private lateinit var fpsText: TextView
    private lateinit var toggleButton: Button

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var imageReader: ImageReader
    private val cameraHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    private lateinit var glRenderer: GLRenderer
    private var isProcessed = true // true for processed, false for raw

    companion object {
        init {
            System.loadLibrary("flamapp") // matches CMake add_library name
        }
    }

    external fun processFrameNV21(input: ByteArray, width: Int, height: Int): ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.texture_view)
        glSurface = findViewById(R.id.gl_surface)
        fpsText = findViewById(R.id.fps_text)
        toggleButton = findViewById(R.id.toggle_button)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                // Will set up camera preview here if needed
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        glSurface.setEGLContextClientVersion(2)
        glRenderer = GLRenderer(640, 480) // use chosen processing resolution
        glSurface.setRenderer(glRenderer)
        glSurface.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        toggleButton.setOnClickListener {
            isProcessed = !isProcessed
            if (isProcessed) {
                textureView.visibility = android.view.View.GONE
                glSurface.visibility = android.view.View.VISIBLE
                toggleButton.text = "Toggle: Processed"
            } else {
                textureView.visibility = android.view.View.VISIBLE
                glSurface.visibility = android.view.View.GONE
                toggleButton.text = "Toggle: Raw"
                // Note: Raw preview not fully implemented, as assignment focuses on processed
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            finish()
        }
    }

    private fun openCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList.first()
        val characteristics = manager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
        val chosen = Size(640, 480)

        imageReader = ImageReader.newInstance(chosen.width, chosen.height, ImageFormat.YUV_420_888, 2)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val nv21 = yuv420ToNV21(image)
            image.close()

            // call native processing (synchronously) - consider doing on background thread
            val start = System.nanoTime()
            val processed = processFrameNV21(nv21, chosen.width, chosen.height)
            val end = System.nanoTime()
            val fps = 1e9 / (end - start)
            runOnUiThread {
                fpsText.text = "Proc ms: ${(end - start)/1e6f} ms"
            }

            // update GL texture (post to GL thread)
            glSurface.queueEvent {
                glRenderer.updateFrame(processed)
            }
        }, cameraHandler)

        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(cam: CameraDevice) {
                cameraDevice = cam
                val surface = imageReader.surface
                val previewRequestBuilder = cam.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                previewRequestBuilder.addTarget(surface)
                cam.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                        session.setRepeatingRequest(previewRequestBuilder.build(), null, cameraHandler)
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, cameraHandler)
            }
            override fun onDisconnected(cam: CameraDevice) { cam.close(); cameraDevice=null }
            override fun onError(cam: CameraDevice, error: Int) { cam.close(); cameraDevice=null }
        }, cameraHandler)
    }

    private fun yuv420ToNV21(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 4
        val nv21 = ByteArray(ySize + uvSize * 2)

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        yBuffer.get(nv21, 0, ySize)

        val rowStride = image.planes[2].rowStride
        val pixelStride = image.planes[2].pixelStride

        val vBytes = ByteArray(vBuffer.remaining())
        vBuffer.get(vBytes)
        val uBytes = ByteArray(uBuffer.remaining())
        uBuffer.get(uBytes)

        var pos = ySize
        // interleave VU
        run {
            var i = 0
            while (i < uvSize) {
                nv21[pos++] = vBytes[i * pixelStride]
                nv21[pos++] = uBytes[i * pixelStride]
                i++
            }
        }
        return nv21
    }

    override fun onDestroy() {
        captureSession?.close()
        cameraDevice?.close()
        imageReader.close()
        super.onDestroy()
    }
}
