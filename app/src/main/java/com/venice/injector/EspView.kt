package com.venice.injector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

class EspView(c: Context) : View(c) {

    companion object { var enabled = true }

    private val box = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.rgb(0, 255, 128)
    }
    private val hpBg = Paint().apply { color = Color.argb(160, 0, 0, 0) }
    private val hpFg = Paint().apply { color = Color.rgb(255, 60, 60) }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        color = Color.argb(180, 0, 255, 128)
    }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 26f
        color = Color.WHITE
        isFakeBoldText = true
    }

    override fun onDraw(canvas: Canvas) {
        postInvalidateDelayed(16)
        if (!enabled || !Esp.running) return

        for (e in Esp.snapshot) {
            val hgt = e.bot - e.top
            if (hgt < 8f) continue
            val wdt = hgt * 0.55f
            val left = e.x - wdt / 2f
            val r = RectF(left, e.top, left + wdt, e.bot)

            canvas.drawRect(r, box)
            canvas.drawLine(e.x, e.bot, width / 2f, height.toFloat(), line)

            val hp = e.hp.coerceIn(0, 100)
            val barTop = e.top - 10f
            canvas.drawRect(left, barTop, left + wdt, barTop + 5f, hpBg)
            canvas.drawRect(left, barTop, left + wdt * (hp / 100f), barTop + 5f, hpFg)

            canvas.drawText("${e.dist}m", left, e.top - 16f, txt)
        }
    }
}
