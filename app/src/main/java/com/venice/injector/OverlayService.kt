package com.venice.injector

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class OverlayService : Service() {

    private val NOTIF_ID = 1
    private val CHANNEL = "vni"

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

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int = START_STICKY

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
            val loaded = ok && (Offsets.load() || Offsets.isReady())
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
            if (!aimOn || !Esp.running) {
                main.postDelayed(this, 120)
                return
            }
            val svc = AimService.instance
            val t = Esp.nearest
            if (svc == null || t == null || aiming || Esp.snapshot.isEmpty()) {
                main.postDelayed(this, 90)
                return
            }
            val dx: Float = t.x - sw / 2f
            val dy: Float = (t.top + t.bot) / 2f - sh / 2f
            if (dx * dx + dy * dy < 144f) {
                main.postDelayed(this, 90)
                return
            }
            aiming = true
            val cx: Float = sw / 2f
            val cy: Float = sh / 2f
            svc.swipeCb(cx, cy, cx + dx * aimSens, cy + dy * aimSens, 45L) {
                main.post { aiming = false }
            }
            main.postDelayed(this, 150)
        }
    }

    private fun startFg() {
        val n = notif()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun fullParams(): WindowManager.LayoutParams {
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.TOP or Gravity.START
        return p
    }

    private fun addFull(isCross: Boolean) {
        val v: View = if (isCross) {
            CrosshairView(this).also { cross = it }
        } else {
            EspView(this).also { esp = it }
        }
        wm.addView(v, fullParams())
        added.add(v)
    }

    private fun addPanel() {
        val d = resources.displayMetrics.density
        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.setBackgroundColor(0xCC101010.toInt())
        panel.setPadding((8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt())

        val title = TextView(this)
        title.text = "vni · cargando"
        title.setTextColor(Color.WHITE)
        title.textSize = 12f
        status = title
        panel.addView(title)

        val swEsp = Switch(this)
        swEsp.text = "ESP"
        swEsp.textSize = 11f
        swEsp.setTextColor(Color.WHITE)
        swEsp.isChecked = true
        swEsp.setOnCheckedChangeListener { _, on -> EspView.enabled = on }
        panel.addView(swEsp)

        val swAim = Switch(this)
        swAim.text = "AIM"
        swAim.textSize = 11f
        swAim.setTextColor(Color.WHITE)
        swAim.setOnCheckedChangeListener { _, on ->
            aimOn = on
            if (on) {
                aiming = false
                main.postDelayed(aimTask, 100)
            }
        }
        panel.addView(swAim)

        val swX = Switch(this)
        swX.text = "X"
        swX.textSize = 11f
        swX.setTextColor(Color.WHITE)
        swX.isChecked = true
        swX.setOnCheckedChangeListener { _, on ->
            CrosshairView.visible = on
            cross?.invalidate()
        }
        panel.addView(swX)

        val barRadius = SeekBar(this)
        barRadius.max = 200
        barRadius.progress = 60
        barRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                CrosshairView.radiusPx = v * d
                cross?.invalidate()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        panel.addView(barRadius)

        val barSens = SeekBar(this)
        barSens.max = 100
        barSens.progress = 50
        barSens.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) {
                aimSens = 0.2f + v / 50f
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        panel.addView(barSens)

        val p = WindowManager.LayoutParams(
            (120 * d).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.TOP or Gravity.START
        p.x = 20
        p.y = 200

        var dx = 0
        var dy = 0
        var px = 0
        var py = 0
        title.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    dx = p.x
                    dy = p.y
                    px = e.rawX.toInt()
                    py = e.rawY.toInt()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = dx + (e.rawX.toInt() - px)
                    p.y = dy + (e.rawY.toInt() - py)
                    wm.updateViewLayout(panel, p)
                    true
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
        for (v in added) {
            try {
                wm.removeView(v)
            } catch (e: Exception) {
            }
        }
        added.clear()
        super.onDestroy()
    }

    private fun notif(): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "VNI", NotificationManager.IMPORTANCE_MIN)
            )
        }
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("vni activo")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }
}
