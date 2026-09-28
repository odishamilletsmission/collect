package org.odk.collect.android.wassan.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class WaveDrawerBackground(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.WHITE          // Drawer background color
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        path.reset()
        path.moveTo(0f, 0f)               // Top-left
        path.lineTo(w - 60f, 0f)          // Top-right

        // Smooth wave using cubic Bezier
        path.cubicTo(
            w + 40f, h * 0.25f,           // Control point 1
            w - 120f, h * 0.75f,           // Control point 2
            w - 60f, h                     // End point at bottom
        )

        path.lineTo(0f, h)                // Bottom-left
        path.close()

        canvas.drawPath(path, paint)
    }
}