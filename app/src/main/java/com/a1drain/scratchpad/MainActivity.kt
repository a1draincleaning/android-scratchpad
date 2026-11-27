package com.a1drain.scratchpad

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
        }

        val startBtn = Button(this).apply {
            text = "Start Floating Pad"
            setOnClickListener { startOverlayIfPermitted() }
        }

        val stopBtn = Button(this).apply {
            text = "Stop Floating Pad"
            setOnClickListener {
                stopService(Intent(this@MainActivity, ScratchpadService::class.java))
                Toast.makeText(this@MainActivity, "Scratchpad stopped", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(startBtn)
        layout.addView(stopBtn)
        setContentView(layout)

        requestNotificationPermissionIfNeeded()
        startOverlayIfPermitted()
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            startScratchpadService()
        }
    }

    private fun startOverlayIfPermitted() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }
        startScratchpadService()
    }

    private fun startScratchpadService() {
        val serviceIntent = Intent(this, ScratchpadService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}
