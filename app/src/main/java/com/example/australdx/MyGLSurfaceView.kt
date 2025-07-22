package com.example.australdx

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import kotlin.math.sqrt
import android.view.ScaleGestureDetector

class MyGLSurfaceView(context: Context, private val renderer: MyGLRenderer) : GLSurfaceView(context) {

    private var previousX = 0f
    private var previousY = 0f

    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            // scaleFactor > 1 => zoom avant, < 1 => zoom arrière

            // Calcule un delta de zoom relatif
            val zoomDelta = (scaleFactor - 1) * 5f  // facteur de sensibilité ajustable

            // Déplace la caméra selon yaw/pitch dans la direction avant/arrière
            renderer.zoomCamera(zoomDelta)

            return true
        }
    })

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(e)

        // Si on n’est pas en zoom (un seul doigt), gérer la rotation
        if (!scaleGestureDetector.isInProgress) {
            val x = e.x
            val y = e.y

            when (e.action) {
                MotionEvent.ACTION_MOVE -> {
                    val dx = x - previousX
                    val dy = y - previousY

                    renderer.rotateCamera(dx * 0.5f, -dy * 0.5f)
                }
            }

            previousX = x
            previousY = y
        }

        return true
    }
}