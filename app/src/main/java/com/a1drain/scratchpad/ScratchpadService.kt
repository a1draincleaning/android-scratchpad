package com.a1drain.scratchpad

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class ScratchpadService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var padView: View? = null

    override fun onCreate() {
        super.onCreate()
        enterForeground()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }

    private fun enterForeground() {
        val channelId = "scratchpad_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Floating Scratchpad",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("A-1 Scratchpad Active")
            .setContentText("Tap the floating bubble anytime to jot notes")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun showBubble() {
        val button = Button(this).apply {
            text = "Jot"
            setBackgroundColor(0xFF1E88E5.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 300
        }

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false

        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(button, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) openScratchpad()
                    true
                }
                else -> false
            }
        }

        bubbleView = button
        windowManager.addView(button, params)
    }

    private fun openScratchpad() {
        if (padView != null) return

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xF2121212.toInt())
            setPadding(20, 20, 20, 20)
        }

        val canvasView = DrawingCanvasView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setBackgroundColor(0xFF1E1E1E.toInt())
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val btnParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            setMargins(6, 0, 6, 12)
        }

        val undoBtn = Button(this).apply {
            text = "Undo"
            layoutParams = btnParams
            setOnClickListener { canvasView.undo() }
        }

        val clearBtn = Button(this).apply {
            text = "Clear"
            layoutParams = btnParams
            setOnClickListener { canvasView.clear() }
        }

        val saveBtn = Button(this).apply {
            text = "Save"
            layoutParams = btnParams
            setOnClickListener {
                val saved = canvasView.saveToGallery()
                val msg = if (saved) "Saved to Pictures/Scratchpad" else "Nothing to save"
                Toast.makeText(this@ScratchpadService, msg, Toast.LENGTH_SHORT).show()
            }
        }

        val hideBtn = Button(this).apply {
            text = "Hide"
            layoutParams = btnParams
            setOnClickListener { closeScratchpad() }
        }

        toolbar.addView(undoBtn)
        toolbar.addView(clearBtn)
        toolbar.addView(saveBtn)
        toolbar.addView(hideBtn)

        container.addView(toolbar)
        container.addView(canvasView)

        val padParams = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            (resources.displayMetrics.heightPixels * 0.6).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        padView = container
        windowManager.addView(container, padParams)
    }

    private fun closeScratchpad() {
        padView?.let { windowManager.removeView(it) }
        padView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        closeScratchpad()
        bubbleView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
