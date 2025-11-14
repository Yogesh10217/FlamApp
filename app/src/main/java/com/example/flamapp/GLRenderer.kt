package com.example.flamapp

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRenderer(private val texWidth: Int, private val texHeight: Int) : GLSurfaceView.Renderer {
    private var textureId = -1
    private var quad: FullScreenQuad? = null
    private var pendingFrame: ByteArray? = null
    private val lock = Object()

    fun updateFrame(rgbaBytes: ByteArray) {
        synchronized(lock) {
            pendingFrame = rgbaBytes
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        textureId = createTexture()
        quad = FullScreenQuad()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        synchronized(lock) {
            pendingFrame?.let { frame ->
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
                val buf = ByteBuffer.wrap(frame)
                GLES20.glTexSubImage2D(
                    GLES20.GL_TEXTURE_2D, 0, 0, 0,
                    texWidth, texHeight,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf
                )
                pendingFrame = null
            }
        }

        quad?.draw(textureId)
    }

    private fun createTexture(): Int {
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0])
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
            texWidth, texHeight, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )
        return tex[0]
    }
}