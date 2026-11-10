package dev.pocketpc.system.capability.android

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20

/**
 * Creates a real EGL context and reads GL_VENDOR / GL_RENDERER / GL_VERSION.
 *
 * This is a genuine hardware query: an offscreen pbuffer context is created against
 * the platform's graphics driver. If no context can be created we report null rather
 * than guessing a renderer name from the SoC marketing string.
 *
 * Note: presence of a renderer string is evidence that a driver exists. It is NOT
 * evidence that our code reaches full frame rate on the GPU; that requires the
 * compositor benchmark in Phase 4.
 */
internal object EglGraphicsProbe {

    data class GlInfo(val vendor: String?, val renderer: String?, val version: String?, val error: String?)

    fun probe(): GlInfo {
        var display: EGLDisplay? = null
        var context: EGLContext? = null
        var surface: EGLSurface? = null
        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return GlInfo(null, null, null, "eglGetDisplay failed")

            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
                return GlInfo(null, null, null, "eglInitialize failed (0x${Integer.toHexString(EGL14.eglGetError())})")
            }

            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 16,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfig = IntArray(1)
            if (!EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfig, 0) || numConfig[0] == 0) {
                return GlInfo(null, null, null, "eglChooseConfig found no ES2 pbuffer config")
            }
            val config = configs[0] ?: return GlInfo(null, null, null, "eglChooseConfig returned null config")

            // Request ES3 where available; fall back to ES2 so the probe still reports a renderer.
            context = createContext(display, config, 3) ?: createContext(display, config, 2)
                ?: return GlInfo(null, null, null, "eglCreateContext failed for ES3 and ES2")

            val pbufferAttribs = intArrayOf(
                EGL14.EGL_WIDTH, 1,
                EGL14.EGL_HEIGHT, 1,
                EGL14.EGL_NONE,
            )
            surface = EGL14.eglCreatePbufferSurface(display, config, pbufferAttribs, 0)
            if (surface == EGL14.EGL_NO_SURFACE) {
                return GlInfo(null, null, null, "eglCreatePbufferSurface failed")
            }
            if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
                return GlInfo(null, null, null, "eglMakeCurrent failed")
            }

            return GlInfo(
                vendor = GLES20.glGetString(GLES20.GL_VENDOR),
                renderer = GLES20.glGetString(GLES20.GL_RENDERER),
                version = GLES20.glGetString(GLES20.GL_VERSION),
                error = null,
            )
        } catch (t: Throwable) {
            return GlInfo(null, null, null, "EGL probe threw ${t.javaClass.simpleName}: ${t.message}")
        } finally {
            runCatching {
                val d = display
                if (d != null && d != EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(d, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    surface?.let { if (it != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(d, it) }
                    context?.let { if (it != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(d, it) }
                    EGL14.eglTerminate(d)
                }
            }
        }
    }

    private fun createContext(display: EGLDisplay, config: EGLConfig, clientVersion: Int): EGLContext? {
        val attribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, clientVersion, EGL14.EGL_NONE)
        val ctx = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, attribs, 0)
        return if (ctx == EGL14.EGL_NO_CONTEXT) null else ctx
    }
}
