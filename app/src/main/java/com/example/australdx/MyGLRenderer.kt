package com.example.australdx

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class MyGLRenderer : GLSurfaceView.Renderer {

    lateinit var glSurfaceView: GLSurfaceView

    // ✅ Optimisation 1: Buffer réutilisable au lieu de listes
    private var vertexBuffer: FloatBuffer? = null
    private var colorBuffer: FloatBuffer? = null
    private var pointCount = 0

    // ✅ Optimisation 2: Shaders compilés une seule fois
    private var shaderProgram = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var mvpMatrixHandle = 0

    private val mVPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    var cameraX = 0f
    var cameraY = 0f
    var cameraZ = 3f

    private var yaw = 500f
    private var pitch = -3.5f


    // ✅ Shaders optimisés pour les points
    private val vertexShaderCode = """
        attribute vec4 vPosition;
        attribute vec4 vColor;
        uniform mat4 uMVPMatrix;
        varying vec4 v_Color;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
            gl_PointSize = 2.0;
            v_Color = vColor;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec4 v_Color;
        void main() {
            gl_FragColor = v_Color;
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        // ✅ Optimisation 3: Compiler les shaders une seule fois
        setupShaders()

    }

    private fun setupShaders() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        shaderProgram = GLES20.glCreateProgram().also { program ->
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
        }

        // Récupérer les handles des attributs/uniformes
        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
        colorHandle = GLES20.glGetAttribLocation(shaderProgram, "vColor")
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 10f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Calcul matrice vue
        updateViewMatrix()
        Matrix.multiplyMM(mVPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)


        // ✅ Optimisation 4: Dessiner tous les points en une seule fois
        drawAllPoints()
    }

    private fun updateViewMatrix() {
        val yawRad = Math.toRadians(yaw.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitch.toDouble()).toFloat()

        val dirX = (Math.cos(pitchRad.toDouble()) * Math.sin(yawRad.toDouble())).toFloat()
        val dirY = Math.sin(pitchRad.toDouble()).toFloat()
        val dirZ = (Math.cos(pitchRad.toDouble()) * Math.cos(yawRad.toDouble())).toFloat()

        val lookX = cameraX + dirX
        val lookY = cameraY + dirY
        val lookZ = cameraZ + dirZ

        Matrix.setLookAtM(
            viewMatrix, 0,
            cameraX, cameraY, cameraZ,
            lookX, lookY, lookZ,
            0f, 1f, 0f
        )
        Log.d("CameraStats", """
        Position: (X=$cameraX, Y=$cameraY, Z=$cameraZ) Yaw: $yaw°, Pitch: $pitch° Direction vector: (X=$dirX, Y=$dirY, Z=$dirZ) LookAt point: (X=$lookX, Y=$lookY, Z=$lookZ) """.trimIndent())

    }

    private fun drawAllPoints() {
        if (vertexBuffer == null || pointCount == 0) return

        // Utiliser le programme shader
        GLES20.glUseProgram(shaderProgram)

        // ✅ Passer la matrice MVP
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mVPMatrix, 0)

        // ✅ Configurer les attributs de position
        GLES20.glEnableVertexAttribArray(positionHandle)
        vertexBuffer?.position(0)
        GLES20.glVertexAttribPointer(
            positionHandle, 3,
            GLES20.GL_FLOAT, false,
            0, vertexBuffer
        )

        // ✅ Configurer les attributs de couleur
        GLES20.glEnableVertexAttribArray(colorHandle)
        colorBuffer?.position(0)
        GLES20.glVertexAttribPointer(
            colorHandle, 4,
            GLES20.GL_FLOAT, false,
            0, colorBuffer
        )

        // ✅ Dessiner tous les points d'un coup !
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pointCount)

        // Nettoyer
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    // ✅ Optimisation 5: Mise à jour directe des buffers
    fun updatePoints(sendedPoints: FloatBuffer) {
        pointCount = sendedPoints.limit() / 3

        if (pointCount == 0) return

        // ✅ Créer buffers de vertices (positions)
        val vertices = FloatArray(pointCount * 3)
        sendedPoints.position(0)
        sendedPoints.get(vertices)

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertices)
                position(0)
            }

        // ✅ Créer buffer de couleurs (toutes rouges pour l'instant)
        val colors = FloatArray(pointCount * 4)
        for (i in 0 until pointCount) {
            colors[i * 4 + 0] = 1f     // R
            colors[i * 4 + 1] = 0f     // G
            colors[i * 4 + 2] = 0f     // B
            colors[i * 4 + 3] = 1f     // A
        }

        colorBuffer = ByteBuffer.allocateDirect(colors.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(colors)
                position(0)
            }

        Log.d("updatePoints", "Mis à jour ${pointCount} points")
    }

    fun rotateCamera(dx: Float, dy: Float) {
        yaw += dx
        pitch += dy
        pitch = pitch.coerceIn(-89f, 89f)
    }

    fun moveCamera(dx: Float, dy: Float, dz: Float) {
        cameraX += dx
        cameraY += dy
        cameraZ += dz
    }

    fun zoomCamera(delta: Float) {
        // Convertir yaw et pitch en radians
        val yawRad = Math.toRadians(yaw.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())

        // Calcul du vecteur direction
        val dirX = (Math.cos(pitchRad) * Math.sin(yawRad)).toFloat()
        val dirY = (Math.sin(pitchRad)).toFloat()
        val dirZ = (Math.cos(pitchRad) * Math.cos(yawRad)).toFloat()

        // Avancer/reculer selon delta * direction
        cameraX += dirX * delta
        cameraY += dirY * delta
        cameraZ += dirZ * delta
    }

    companion object {
        fun loadShader(type: Int, shaderCode: String): Int {
            return GLES20.glCreateShader(type).also { shader ->
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)
            }
        }
    }
}