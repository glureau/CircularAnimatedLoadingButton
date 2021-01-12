package com.glureau.circularanimatedloadingbutton

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.annotation.FloatRange
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

class CircularAnimatedLoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val borderPath = Path()
    private val borderPaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.FILL_AND_STROKE
    }

    private val progressionPath = Path()
    private val progressionPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL_AND_STROKE
    }

    private val backgroundPath = Path()
    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL_AND_STROKE
    }

    private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
        .apply {
            duration = 3000 // could be parsed from view attributes
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                computePath(animation.animatedValue as Float)
                invalidate()
            }
        }

    fun start() {
        animator.start()
    }

    private fun computePath(@FloatRange(from = 0.0, to = 1.0) animation: Float) {
        borderPath.reset()
        progressionPath.reset()
        backgroundPath.reset()

        val extRadius = height / 2f

        // Constants could come from resources or parsed from view attributes...
        val progressionPercent = 0.3f // percent of the border will be the progression
        val borderWidth = 12f//px

        // First path consist to a rectangle + 2 arcs, coloring the background
        borderPath.addRect(
            extRadius,
            0f,
            width - extRadius,
            height.toFloat(),
            Path.Direction.CW
        )
        borderPath.addArc(width - 2 * extRadius, 0f, width.toFloat(), height.toFloat(), -90f, 180f)
        borderPath.addArc(0f, 0f, extRadius * 2f, height.toFloat(), 90f, 180f)

        // Second path is the progression the will partially recover the border
        val halfCircleLength = PI.toFloat() * extRadius
        val circleLength = 2 * halfCircleLength
        val segmentLength = (width - height).toFloat()
        val fullLength = segmentLength * 2 + circleLength
        val tailPos: Float = animation * fullLength
        val headPos: Float = (animation + progressionPercent) * fullLength
        // Let's say 'start' is the left start of the top segment, and we animate clockwise
        if (tailPos < segmentLength) {
            progressionPath.addRect(
                extRadius + min(tailPos, segmentLength),
                0f,
                extRadius + min((headPos % fullLength), segmentLength),
                borderWidth,
                Path.Direction.CW
            )
        }
        // Right half circle
        if (headPos >= segmentLength && tailPos < segmentLength + halfCircleLength) {
            val startAngle =
                180 * (min(
                    max(tailPos, segmentLength) - segmentLength,
                    halfCircleLength
                ) / halfCircleLength)
            val sweepTailAngle =
                180 * (min(tailPos - segmentLength, halfCircleLength) / halfCircleLength)
            val sweepHeadAngle =
                180 * (min(headPos - segmentLength, halfCircleLength) / halfCircleLength)
            val sweepAngle =
                (min(sweepHeadAngle, sweepHeadAngle - sweepTailAngle)).coerceIn(0f, 180f)

            progressionPath.arcTo(
                width - 2 * extRadius,
                0f,
                width.toFloat(),
                height.toFloat(),
                -90f + startAngle,
                0f + sweepAngle,
                false
            )

            progressionPath.arcTo(
                width - 2 * extRadius + borderWidth,
                0f + borderWidth,
                width.toFloat() - borderWidth,
                height.toFloat() - borderWidth,
                -90f + startAngle + sweepAngle,
                0f - sweepAngle,
                false
            )
        }
        // Bottom line
        if (headPos >= segmentLength + halfCircleLength && tailPos < 2 * segmentLength + halfCircleLength) {
            progressionPath.addRect(
                width - extRadius - min(
                    (headPos - (segmentLength + halfCircleLength)),
                    segmentLength
                ),
                height - borderWidth,
                width - extRadius - max(
                    (tailPos - (segmentLength + halfCircleLength)),
                    0f
                ),
                height.toFloat(),
                Path.Direction.CW
            )
        }

        // Left half circle
        if ((headPos >= 2 * segmentLength + halfCircleLength) &&
            (tailPos < 2 * (segmentLength + circleLength))
        ) {

            val sp = tailPos - (segmentLength + halfCircleLength)
            val ep = headPos - (segmentLength + halfCircleLength)
            val startAngle =
                180 * (min(
                    max(sp, segmentLength) - segmentLength,
                    halfCircleLength
                ) / halfCircleLength)
            val sweepTailAngle =
                180 * (min(sp - segmentLength, halfCircleLength) / halfCircleLength)
            val sweepHeadAngle =
                180 * (min(ep - segmentLength, halfCircleLength) / halfCircleLength)
            val sweepAngle =
                (min(sweepHeadAngle, sweepHeadAngle - sweepTailAngle)).coerceIn(0f, 180f)

            progressionPath.arcTo(
                0f,
                0f,
                2 * extRadius,
                height.toFloat(),
                90f + startAngle,
                0f + sweepAngle,
                false
            )

            progressionPath.arcTo(
                borderWidth,
                borderWidth,
                2 * extRadius - borderWidth,
                height.toFloat() - borderWidth,
                90f + startAngle + sweepAngle,
                0f - sweepAngle,
                false
            )
        }

        // Could be merged with the 1st part (top border)
        if (headPos > fullLength) {
            progressionPath.addRect(
                extRadius + max(0f, tailPos - fullLength),
                0f,
                extRadius + min(headPos % fullLength, segmentLength),
                borderWidth,
                Path.Direction.CW
            )
        }

        // Last pass, draw the text background
        backgroundPath.addRect(
            extRadius,
            borderWidth,
            width - extRadius,
            height.toFloat() - borderWidth,
            Path.Direction.CW
        )
        backgroundPath.addArc(
            width - 2 * extRadius + borderWidth,
            borderWidth,
            width.toFloat() - borderWidth,
            height.toFloat() - borderWidth,
            -90f,
            180f
        )
        backgroundPath.addArc(
            borderWidth,
            borderWidth,
            extRadius * 2f - borderWidth,
            height.toFloat() - borderWidth,
            90f,
            180f
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(borderPath, borderPaint)
        canvas.drawPath(progressionPath, progressionPaint)
        canvas.drawPath(backgroundPath, backgroundPaint)
        super.draw(canvas)
    }
}