package com.venice.injector

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private var cross: CrosshairView? = null
    private var esp: EspView? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, notif())
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = resources.displayMetrics

        val ok = Root.hasRoot() && Root.attach()

        addFull(cross = true)
        addFull(cross = false)
        if (ok) Esp.start(dm.widthPixels, dm.heightPixels)
        addPanel(ok)
    }

    private fun fullParams(extra: Int) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or extra,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun addFull(cross: Boolean) {
        if (cross) {
            val v = CrosshairView(this)
            wm.addView(v, fullParams(0))
            this.cross = v
        } else {
            val v = EspView(this)
            wm.addView(v, fullParams(0))
            this.esp = v
        }
    }

    private fun addPanel(rootOk: Boolean) {
        val d = resources.displayMetrics.density
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xCC101010.toInt())
            setPadding((8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt())
        }

        val title = TextView(this).apply {
            text = if (rootOk) "·" else "· sin root"
            setTextColor(if (rootOk) Color.WHITE else Color.RED)
            textSize = 13f
        }
        panel.addView(title)

        panel.addView(Switch(this).apply {
            text = "ESP"
            textSize = 11f
            setTextColor(Color.WHITE)
            isChecked = true
            setOnCheckedChangeListener { _, on -> EspView.enabled = on }
        })

        panel.addView(Switch(this).apply {
            text = "X"
            textSize = 11f
            setTextColor(Color.WHITE)
            isChecked = true
            setOnCheckedChangeListener { _, on ->
                CrosshairView.visible = on
                cross?.invalidate()
            }
        })

        panel.addView(SeekBar(this).apply {
            max = 200
            progress = 60
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                    CrosshairView.radiusPx = v * d
                    cross?.invalidate()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })

        panel.addView(Button(this).apply {
            text = "TAP"
            textSize = 10f
            setOnClickListener {
                val dm = resources.displayMetrics
                AimService.instance?.tap(dm.widthPixels / 2f, dm.heightPixels / 2f)
            }
        })

        panel.addView(Button(this).apply {
            text = "PULL"
            textSize = 10f
            setOnClickListener {
                val dm = resources.displayMetrics
                val x = dm.widthPixels / 2f
                val y = dm.heightPixels / 2f
                AimService.instance?.swipe(x, y, x, y + 220 * d, 300)
            }
        })

        val p = WindowManager.LayoutParams(
            (110 * d).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 200
        }

        var dx = 0; var dy = 0; var px = 0; var py = 0
        title.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    dx = p.x; dy = p.y
                    px = e.rawX.toInt(); py = e.rawY.toInt(); true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = dx + (e.rawX.toInt() - px)
                    p.y = dy + (e.rawY.toInt() - py)
                    wm.updateViewLayout(panel, p); true
                }
                else -> false
            }
        }

        wm.addView(panel, p)
    }

    override fun onDestroy() {
        Esp.stop()
        Root.detach()
        super.onDestroy()
    }

    private fun notif(): Notification {
        val ch = NotificationChannel("vni", "VNI", NotificationManager.IMPORTANCE_MIN)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        return Notification.Builder(this, "vni")
            .setContentTitle("on")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }
}
