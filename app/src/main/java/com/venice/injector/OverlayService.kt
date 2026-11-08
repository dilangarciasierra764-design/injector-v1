package com.venice.injector

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.*

class OverlayService : Service() {

    private lateinit var wm: WindowManager
    private val added = ArrayList<View>()
    private var cross: CrosshairView? = null
    private var esp: EspView? = null
    private var status: TextView? = null

    private val main = Handler(Looper.getMainLooper())
    private var aimOn = false
    private var aiming = false
    private var aimSens = 1.0f
    private var sw = 0
    private var sh = 0

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startFg()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val dm = resources.displayMetrics
        sw = dm.widthPixels
        sh = dm.heightPixels

        addFull(true)
        addFull(false)
        addPanel()

        Thread {
            val ok = Root.hasRoot() && Root.attach()
            val loaded = ok && Offsets.load()
            main.post {
                status?.text = when {
                    !ok -> "sin root"
                    !loaded -> "offsets: faltan"
                    else -> "listo"
                }
                if (ok && loaded) Esp.start(sw, sh) else Esp.error = if (!ok) "sin root" else "offsets"
            }
        }.start()

        main.postDelayed(statusTask, 500)
        main.postDelayed(aimTask, 400)
    }

    private val statusTask = object : Runnable {
        override fun run() {
            status?.text = if (!Root.ready) "sin root"
            else if (!Offsets.isReady()) "offsets: faltan"
            else Esp.error.ifEmpty { "listo" }
            main.postDelayed(this, 500)
        }
    }

    private val aimTask = object : Runnable {
        override fun run() {
            if (!aimOn) return
            val svc = AimService.instance
            val t = Esp.nearest
            if (svc == null || t == null || aiming || Esp.snapshot.isEmpty()) {
                main.postDelayed(this, 90)
                return
            }
            val dx = t.x - sw / 2f
            val dy = (t.top + t.bot) / 2f - sh / 2f
            if (dx * dx + dy * dy < 144f) { main.postDelayed(this, 90); return }
            aiming = true
            val cx = sw / 2f
            val cy = sh / 2f
            svc.swipeCb(cx, cy, cx + dx * aimSens, cy + dy * aimSens, 45) {
                main.post { aiming = false }
            }
            main.postDelayed(this, 150)
        }
    }

    private fun startFg() {
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, notif(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, notif())
    }

    private fun overlayType() =
        if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun fullParams(extra: Int) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or extra,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun addFull(isCross: Boolean) {
        val v: View = if (isCross) CrosshairView(this).also { cross = it } else EspView(this).also { esp = it }
        wm.addView(v, fullParams(0))
        added.add(v)
    }

    private fun addPanel() {
        val d = resources.displayMetrics.density
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xCC101010.toInt())
            setPadding((8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt())
        }

        val title = TextView(this).apply {
            text = "vni · cargando"
            setTextColor(Color.WHITE)
            textSize = 12f
        }
        status = title
        panel.addView(title)

        panel.addView(Switch(this).apply {
            text = "ESP"; textSize = 11f; setTextColor(Color.WHITE); isChecked = true
            setOnCheckedChangeListener { _, on -> EspView.enabled = on }
        })

        panel.addView(Switch(this).apply {
            text = "AIM"; textSize = 11f; setTextColor(Color.WHITE)
            setOnCheckedChangeListener { _, on ->
                aimOn = on
                if (on) { aiming = false; main.postDelayed(aimTask, 100) }
            }
        })

        panel.addView(Switch(this).apply {
            text = "X"; textSize = 11f; setTextColor(Color.WHITE); isChecked = true
            setOnCheckedChangeListener { _, on -> CrosshairView.visible = on; cross?.invalidate() }
        })

        panel.addView(SeekBar(this).apply {
            max = 200; progress = 60
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                    CrosshairView.radiusPx = v * d
                    cross?.invalidate()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })

        panel.addView(SeekBar(this).apply {
            max = 100; progress = 50
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                    aimSens = 0.2f + v / 50f
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })

        val p = WindowManager.LayoutParams(
            (120 * d).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 20; y = 200 }

        var dx = 0; var dy = 0; var px = 0; var py = 0
        title.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    dx = p.x; dy = p.y; px = e.rawX.toInt(); py = e.rawY.toInt(); true
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
        added.add(panel)
    }

    override fun onDestroy() {
        aimOn = false
        main.removeCallbacksAndMessages(null)
        Esp.stop()
        Root.detach()
        for (v in added) try { wm.removeView(v) } catch (_: Exception) {}
        added.clear()
        super.onDestroy()
    }

    private fun notif(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("vni", "VNI", NotificationManager.IMPORTANCE_MIN))
        return Notification.Builder(this, "vni")
            .setContentTitle("vni activo")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }
}
