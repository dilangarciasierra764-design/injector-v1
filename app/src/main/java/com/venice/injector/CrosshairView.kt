package com.venice.injector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class CrosshairView(c: Context) : View(c) {

    companion object {
        @Volatile var visible = true
        @Volatile var radiusPx = 60f
    }

    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(220, 0, 255, 128)
    }

    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 255, 128)
    }

    override fun onDraw(canvas: Canvas) {
        postInvalidateDelayed(16)
        if (!visible) return
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, radiusPx, ring)
        canvas.drawCircle(cx, cy, 4f, dot)
    }
}
