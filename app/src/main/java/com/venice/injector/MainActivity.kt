package com.venice.injector

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        }

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_VERTICAL
        root.setPadding(pad, pad * 4, pad, pad)

        status = TextView(this)
        status.textSize = 14f
        status.setTextColor(Color.DKGRAY)
        root.addView(status)

        root.addView(button("1 · overlay") {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        })

        root.addView(button("2 · accesibilidad") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })

        root.addView(button("3 · conceder root") {
            thread {
                Root.sh("id")
                runOnUiThread { refresh() }
            }
        })

        root.addView(button("cargar offsets") {
            thread {
                val ok = Offsets.load()
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        if (ok) "offsets: ${Offsets.source}" else "no se pudo leer offsets.json",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })

        root.addView(button("4 · iniciar") {
            if (!Settings.canDrawOverlays(this@MainActivity)) return@button
            startForegroundService(Intent(this@MainActivity, OverlayService::class.java))
            moveTaskToBack(true)
        })

        root.addView(button("detener") {
            stopService(Intent(this@MainActivity, OverlayService::class.java))
        })

        setContentView(root)
        refresh()
    }

    private fun button(txt: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = txt
        b.setOnClickListener { onClick() }
        return b
    }

    private fun refresh() {
        val ov = Settings.canDrawOverlays(this)
        val a11y = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
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
}
