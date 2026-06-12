package com.dawncaster.translator

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * Плавающее окно, которое показывает оригинал + перевод поверх игры.
 * Полупрозрачное, перетаскиваемое, само скрывается через 4 секунды.
 */
object FloatingWindow {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var hideRunnable: Runnable? = null

    /** Показать перевод поверх всех приложений */
    fun show(context: Context, original: String, translation: String) {
        hide(context)  // убрать предыдущее окно, если висит

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val inflater = LayoutInflater.from(context)
        overlayView = inflater.inflate(android.R.layout.simple_list_item_2, null)

        val titleView = overlayView!!.findViewById<TextView>(android.R.id.text1)
        val subtitleView = overlayView!!.findViewById<TextView>(android.R.id.text2)

        titleView?.apply {
            text = original.take(200)
            textSize = 13f
            setTextColor(0xFFCCCCCC.toInt())
        }
        subtitleView?.apply {
            text = translation.take(300)
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
        }

        overlayView?.setBackgroundColor(0xCC1A1A2E.toInt())
        overlayView?.setPadding(24, 16, 24, 16)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 160  // отступ от низа, чтобы не перекрывать кнопки игры
        }

        windowManager?.addView(overlayView, params)

        // Автоскрытие через 4 секунды
        hideRunnable = Runnable { hide(context) }
        overlayView?.postDelayed(hideRunnable, 4000)
    }

    /** Убрать окно */
    fun hide(context: Context) {
        hideRunnable?.let { overlayView?.removeCallbacks(it) }
        try {
            overlayView?.let {
                windowManager?.removeView(it)
            }
        } catch (_: Exception) {}
        overlayView = null
        windowManager = null
        hideRunnable = null
    }
}
