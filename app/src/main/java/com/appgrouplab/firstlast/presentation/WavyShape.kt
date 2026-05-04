package com.appgrouplab.firstlast.presentation

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection


// 1. La Forma Personalizada para la Onda (Custom Shape)
class WavyShape(
    private val splitYPosition: Float = 0.5f, // Posición vertical de la onda (0.4f para 40%)
    private val period: Float = 2f,
    private val amplitude: Float = 80f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val waveCenterY = size.height * splitYPosition // Usamos la nueva posición

        path.moveTo(0f, 0f)
        path.lineTo(size.width, 0f)
        path.lineTo(size.width, waveCenterY)

        val waveWidth = size.width / period
        for (i in 0 until period.toInt()) {
            val startX = size.width - (i * waveWidth)
            val controlX = startX - (waveWidth / 2f)
            val endX = startX - waveWidth

            val controlY = if (i % 2 == 0) {
                waveCenterY - amplitude
            } else {
                waveCenterY + amplitude
            }

            val finalControlY = controlY.coerceIn(0f, size.height)

            path.quadraticBezierTo(
                x1 = controlX, y1 = finalControlY,
                x2 = endX, y2 = waveCenterY
            )
        }

        path.lineTo(0f, waveCenterY)
        path.close()

        return Outline.Generic(path)
    }
}