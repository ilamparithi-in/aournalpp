package dev.ilamparithi.aournalpp.x11

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.x11.LorieView
import com.termux.x11.Prefs
import com.termux.x11.input.InputEventSender
import com.termux.x11.input.LenovoPenButtonMapper
import com.termux.x11.input.TouchInputHandler

private fun findActivity(context: Context): Activity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun X11Viewport(
    modifier: Modifier = Modifier,
    onLorieViewReady: (LorieView) -> Unit,
    onInputHandlerReady: ((TouchInputHandler) -> Unit)? = null
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            val activity = findActivity(context)
            val prefs = Prefs(context)

            val frameLayout = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isFocusable = true
                isFocusableInTouchMode = true
            }

            val lorieView = LorieView(context).apply {
                id = com.termux.x11.R.id.lorieView
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isFocusable = true
                isFocusableInTouchMode = true
            }

            frameLayout.addView(lorieView)
            lorieView.requestFocus()

            if (activity != null) {
                lorieView.requestStylusEnabled(true)
                val inputSender = InputEventSender(lorieView)
                val inputHandler = TouchInputHandler(activity, inputSender)
                inputHandler.reloadPreferences(prefs)

                val penMapper = LenovoPenButtonMapper(activity, inputHandler)
                penMapper.reloadPreferences(prefs)

                frameLayout.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        frameLayout.requestUnbufferedDispatch(event)
                    }
                    inputHandler.handleTouchEvent(frameLayout, lorieView, event)
                }
                frameLayout.setOnHoverListener { _, event ->
                    inputHandler.handleTouchEvent(frameLayout, lorieView, event)
                }
                frameLayout.setOnGenericMotionListener { _, event ->
                    inputHandler.handleTouchEvent(frameLayout, lorieView, event)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    lorieView.setOnCapturedPointerListener { _, event ->
                        inputHandler.handleTouchEvent(lorieView, lorieView, event)
                    }
                    frameLayout.setOnCapturedPointerListener { _, event ->
                        inputHandler.handleTouchEvent(lorieView, lorieView, event)
                    }
                }
                lorieView.setOnKeyListener { _, keyCode, event ->
                    if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                        return@setOnKeyListener false
                    }
                    if (penMapper.onKeyEvent(event)) {
                        return@setOnKeyListener true
                    }
                    inputHandler.sendKeyEvent(event)
                }
                frameLayout.setOnKeyListener { _, keyCode, event ->
                    if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                        return@setOnKeyListener false
                    }
                    if (penMapper.onKeyEvent(event)) {
                        return@setOnKeyListener true
                    }
                    inputHandler.sendKeyEvent(event)
                }
                lorieView.setCallback { screenWidth, screenHeight, inputTransform ->
                    inputHandler.handleInputTransformChanged(screenWidth, screenHeight, inputTransform)
                }

                onInputHandlerReady?.invoke(inputHandler)
            }

            onLorieViewReady(lorieView)
            frameLayout
        }
    )
}
