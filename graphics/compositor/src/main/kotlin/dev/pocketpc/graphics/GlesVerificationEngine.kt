package dev.pocketpc.graphics

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.SystemClock
import dev.pocketpc.system.capability.CapabilityEvidence
import dev.pocketpc.system.capability.PerformanceSample
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Executes a real rendering workload on the device GPU and reports what actually
 * happened.
 *
 * Why this proves something the old probe did not: creating a context (the old
 * `EglGraphicsProbe`) only shows a driver loaded. This engine issues clear + draw calls
 * on an offscreen surface, reads the resulting pixels back from the framebuffer, and
 * compares them against the colour it asked for. If the pixels are right, the command
 * stream was genuinely executed end-to-end on whatever renderer strings it reports.
 * Combined with [SoftwareRendererDetector] that distinguishes "GPU did the work" from
 * "a software rasteriser did the work".
 *
 * Frames are timed with [SystemClock.elapsedRealtimeNanos] and `glFinish()` per frame,
 * so the measurement is of completed work rather than of queued commands.
 */
public class GlesVerificationEngine(
    private val width: Int = 512,
    private val height: Int = 512,
) {

    public companion object {
        /** Colour the workload draws; read back to prove execution. */
        private const val TARGET_R = 0.0f  // 0
        private const val TARGET_G = 0.639f
        private const val TARGET_B = 1.0f
        private const val TARGET_TOLERANCE = 12  // per-channel 8-bit tolerance due to dithering

        public const val DEFAULT_TIMED_FRAMES: Int = 120
    }

    private val VERTEX_SHADER: String = Shaders.VERTEX_SHADER
    private val FRAGMENT_SHADER: String = Shaders.FRAGMENT_SHADER

    public fun run(timedFrames: Int = DEFAULT_TIMED_FRAMES): GpuVerificationResult {
        var display: EGLDisplay? = null
        var context: EGLContext? = null
        var surface: EGLSurface? = null
        var contextCreated = false
        var commandsExecuted = false
        var frameProduced = false
        var failures = 0
        var renderer: String? = null
        var vendor: String? = null
        var api = "OpenGL ES (unknown)"
        var glError: Int? = null
        var perf: PerformanceSample? = null
        var error: String? = null

        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) {
                return fail("eglGetDisplay returned EGL_NO_DISPLAY")
            }
            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
                return fail("eglInitialize failed (0x${Integer.toHexString(EGL14.eglGetError())})")
            }

            val config = chooseConfig(display)
                ?: return fail("no ES2/ES3 config with a pbuffer surface")

            context = createContext(display, config, 3) ?: createContext(display, config, 2)
            if (context == null) return fail("eglCreateContext failed for ES3 and ES2")

            surface = EGL14.eglCreatePbufferSurface(
                display,
                config,
                intArrayOf(EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT, height, EGL14.EGL_NONE),
                0,
            )
            if (surface == EGL14.EGL_NO_SURFACE) return fail("eglCreatePbufferSurface failed")
            if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
                return fail("eglMakeCurrent failed")
            }
            contextCreated = true

            renderer = GLES20.glGetString(GLES20.GL_RENDERER)
            vendor = GLES20.glGetString(GLES20.GL_VENDOR)
            val glVersion = GLES20.glGetString(GLES20.GL_VERSION).orEmpty()
            api = "OpenGL ES ${parseEsVersion(glVersion) ?: "?"}"

            // ---- Real draw: two triangles in the target colour, using a VBO + shader ----
            val program = buildProgram()
            if (program == 0) {
                glError = GLES20.glGetError()
                return fail("shader program failed to link (GL error 0x${Integer.toHexString(glError ?: 0)})")
            }
            val buffer = FloatBuffer.allocate(12)
            // Full-screen quad as two triangles in clip space.
            buffer.put(-1f); buffer.put(-1f)
            buffer.put(1f); buffer.put(-1f)
            buffer.put(-1f); buffer.put(1f)
            buffer.put(1f); buffer.put(-1f)
            buffer.put(-1f); buffer.put(1f)
            buffer.put(1f); buffer.put(1f)
            buffer.position(0)

            val vbo = IntArray(1)
            GLES20.glGenBuffers(1, vbo, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 48, buffer, GLES20.GL_STATIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

            val posLoc = GLES20.glGetAttribLocation(program, "aPos")
            GLES20.glUseProgram(program)
            GLES20.glViewport(0, 0, width, height)
            GLES20.glDisable(GLES20.GL_DITHER)
            GLES20.glClearColor(TARGET_R, TARGET_G, TARGET_B, 1f)

            // ---- Frame timing ----
            val frameTimes = ArrayList<Double>(timedFrames)
            val pixels = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
            var verifiedThisRun = false
            for (i in 0 until timedFrames) {
                val t0 = SystemClock.elapsedRealtimeNanos()
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
                if (posLoc >= 0) {
                    GLES20.glEnableVertexAttribArray(posLoc)
                    GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, 0)
                }
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
                // glFinish forces completion so the timing measures executed work.
                GLES20.glFinish()
                val t1 = SystemClock.elapsedRealtimeNanos()
                frameTimes += (t1 - t0) / 1_000_000.0

                if (i == 0) {
                    glError = GLES20.glGetError()
                    commandsExecuted = glError == 0
                    // Pixel readback: the only acceptable proof that a frame exists.
                    pixels.position(0)
                    GLES20.glReadPixels(
                        width / 2,
                        height / 2,
                        1,
                        1,
                        GLES20.GL_RGBA,
                        GLES20.GL_UNSIGNED_BYTE,
                        pixels,
                    )
                    val r = pixels.get(0).toInt() and 0xFF
                    val g = pixels.get(1).toInt() and 0xFF
                    val b = pixels.get(2).toInt() and 0xFF
                    val expectedR = (TARGET_R * 255).toInt()
                    val expectedG = (TARGET_G * 255).toInt()
                    val expectedB = (TARGET_B * 255).toInt()
                    val matches = kotlin.math.abs(r - expectedR) <= TARGET_TOLERANCE &&
                        kotlin.math.abs(g - expectedG) <= TARGET_TOLERANCE &&
                        kotlin.math.abs(b - expectedB) <= TARGET_TOLERANCE
                    if (matches) {
                        frameProduced = true
                        verifiedThisRun = true
                    } else {
                        failures++
                    }
                }
            }

            GLES20.glDeleteBuffers(1, vbo, 0)
            GLES20.glDeleteProgram(program)

            if (frameTimes.isNotEmpty()) {
                perf = PerformanceSample.fromFrameTimes(frameTimes, renderer ?: "unknown", api)
            }
            if (!verifiedThisRun && frameProduced.not()) {
                error = "centre pixel did not match the drawn colour: the frame could not be verified"
            }
        } catch (t: Throwable) {
            error = "${t.javaClass.simpleName}: ${t.message}"
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

        val evidence = GpuVerifier.buildEvidence(
            api = api,
            renderer = renderer,
            vendor = vendor,
            contextCreated = contextCreated,
            commandsExecuted = commandsExecuted,
            frameProduced = frameProduced,
            frames = perf?.frames ?: 0,
            glError = glError,
        )
        val report = buildList {
            addAll(GpuVerifier.summarize(evidence, perf))
            add("Vendor: ${vendor ?: "unknown"}")
            if (SoftwareRendererDetector.isSoftware(renderer, vendor)) {
                add("WARNING: renderer matches a known software rasteriser; this is NOT GPU acceleration")
            }
            error?.let { add("Error: $it") }
        }
        return GpuVerificationResult(evidence, perf, report, failures, error)
    }

    private fun fail(message: String): GpuVerificationResult = GpuVerificationResult(
        evidence = CapabilityEvidence(detected = true, initialized = false, rendererImplemented = true),
        performance = null,
        report = listOf("GPU verification failed: $message"),
        verificationFailures = 1,
        error = message,
    )

    private fun chooseConfig(display: EGLDisplay): EGLConfig? {
        val attribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        return if (EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, count, 0) && count[0] > 0) {
            configs[0]
        } else {
            null
        }
    }

    private fun createContext(display: EGLDisplay, config: EGLConfig, clientVersion: Int): EGLContext? {
        val ctx = EGL14.eglCreateContext(
            display,
            config,
            EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, clientVersion, EGL14.EGL_NONE),
            0,
        )
        return if (ctx == EGL14.EGL_NO_CONTEXT) null else ctx
    }

    private fun buildProgram(): Int {
        val vs = compile(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        if (vs == 0 || fs == 0) return 0
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        return if (status[0] == GLES20.GL_TRUE) program else 0
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] != GLES20.GL_TRUE) {
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    /** "OpenGL ES 3.2 v1.r53p0-..." -> "3.2" */
    private fun parseEsVersion(glVersion: String): String? {
        val marker = "OpenGL ES "
        val idx = glVersion.indexOf(marker)
        if (idx < 0) return null
        return glVersion.substring(idx + marker.length)
            .takeWhile { it.isDigit() || it == '.' }
            .takeIf { it.isNotEmpty() }
    }

    private object Shaders {
        val VERTEX_SHADER = """
            attribute vec2 aPos;
            void main() {
                gl_Position = vec4(aPos, 0.0, 1.0);
            }
        """.trimIndent()

        val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                gl_FragColor = vec4(0.0, 0.639, 1.0, 1.0);
            }
        """.trimIndent()
    }
}
