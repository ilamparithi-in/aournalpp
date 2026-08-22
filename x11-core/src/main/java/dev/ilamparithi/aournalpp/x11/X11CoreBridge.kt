package dev.ilamparithi.aournalpp.x11

import android.view.Surface

object X11CoreBridge {
    init {
        System.loadLibrary("c++_shared")
        System.loadLibrary("x11core")
    }

    external fun nativeInitServer(socketPath: String): Boolean
    external fun nativeSurfaceCreated(surface: Surface)
    external fun nativeSurfaceChanged(surface: Surface, width: Int, height: Int)
    external fun nativeSurfaceDestroyed()
    external fun nativeSendTouchEvent(action: Int, pointerId: Int, x: Float, y: Float, pressure: Float, toolType: Int)
    external fun nativeSendKeyEvent(keyCode: Int, unicodeChar: Int, isDown: Boolean)
}
