package com.venice.injector

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.concurrent.thread

class MainActivity : Activity() {

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad, pad * 4, pad, pad)
        }

        val status = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.DKGRAY)
        }
        root.addView(status)

        fun refresh() {
            val ov = Settings.canDrawOverlays(this)
            val a11y = Settings.Secure.getString(
                contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )?.contains(packageName) == true
            status.text = "overlay: ${if (ov) "ok" else "falta"}\n" +
                "accesibilidad: ${if (a11y) "ok" else "falta"}\nroot: comprobando..."
            thread {
                val r = Root.hasRoot()
                runOnUiThread {
                    status.text = "overlay: ${if (ov) "ok" else "falta"}\n" +
                        "accesibilidad: ${if (a11y) "ok" else "falta"}\n" +
                        "root: ${if (r) "ok" else "falta"}"
                    status.setTextColor(if (r) Color.rgb(0, 140, 0) else Color.DKGRAY)
                }
            }
        }

        root.addView(Button(this).apply {
            text = "1 · overlay"
            setOnClickListener {
                startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ))
            }
        })

        root.addView(Button(this).apply {
            text = "2 · accesibilidad"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        root.addView(Button(this).apply {
            text = "3 · conceder root"
            setOnClickListener { thread { Root.sh("id"); runOnUiThread { refresh() } } }
        })

        root.addView(Button(this).apply {
            text = "4 · iniciar"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) return@setOnClickListener
                startForegroundService(Intent(this@MainActivity, OverlayService::class.java))
                moveTaskToBack(true)
            }
        })

        root.addView(Button(this).apply {
            text = "detener"
            setOnClickListener { stopService(Intent(this@MainActivity, OverlayService::class.java)) }
        })

        setContentView(root)
        refresh()
    }
}
