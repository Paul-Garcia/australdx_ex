package com.example.australdx

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import android.util.Log


class Point3D(
    public val coords: FloatArray,
    public val color: FloatArray
) {
    constructor(other: Point3D) : this(
        other.coords.copyOf(),
        other.color.copyOf()
    )


    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(coords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(coords)
                position(0)
            }
        }

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        void main() {
            gl_PointSize = 20.0;
            gl_Position = uMVPMatrix * vPosition;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
    """.trimIndent()

    private val mProgram: Int = GLES20.glCreateProgram().also {
        val vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        GLES20.glAttachShader(it, vertexShader)
        GLES20.glAttachShader(it, fragmentShader)
        GLES20.glLinkProgram(it)
    }

    fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(mProgram)
        Log.d("draw", "Drawing point: $this")
        val positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)

        val colorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        val mvpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    override fun toString(): String {
        return "Point3D(x=${coords[0]}, y=${coords[1]}, z=${coords[2]}, r=${color[0]}, g=${color[1]}, b=${color[2]}, a=${color[3]})"
    }
}
