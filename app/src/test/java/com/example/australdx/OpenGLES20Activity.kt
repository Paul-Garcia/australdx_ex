package com.example.australdx

import android.os.Bundle
import android.util.Log
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder

import android.app.Activity


class OpenGLES20Activity : Activity() {

    private lateinit var gLView: GLSurfaceView


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val myRenderer = MyGLRenderer()
        gLView = MyGLSurfaceView(this, myRenderer)
        myRenderer.glSurfaceView = gLView
        setContentView(gLView)

        SocketHandler.setSocket()
        SocketHandler.establishConnection()
        val mSocket = SocketHandler.getSocket()

        mSocket.on("pointCloud") { args ->
            val buffer = args[0] as ByteArray
            val floatBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            gLView.queueEvent {
                myRenderer.updatePoints(floatBuffer)
            }
        }
    }



    protected override fun onPause() {
        super.onPause()
    }

    protected override fun onResume() {
        super.onResume()
    }
}


