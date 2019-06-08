package io.stanc.pogotool.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log

object IconFactory {
    private val TAG = javaClass.name

    data class IconSizeConfig (
        val backgroundSize: Int,
        val foregroundSize: Int
    )

    data class DrawableConfig (
        val drawable: Drawable,
        val size: Int
    )

    data class IconConfig (
        val backgroundConfig: DrawableConfig,
        var foregroundConfig: DrawableConfig? = null,
        var headerText: String? = null
    )

    private const val TEXT_RAW_SIZE: Float = 10.0f
    private const val TEXT_COLOR = Color.BLACK

    fun bitmap(context: Context, iconConfig: IconConfig): Bitmap {

        val bitmap = createBitmap(context, iconConfig)
        val canvas = Canvas(bitmap)

        // TODO: for debugging
//        canvas.drawColor(Color.YELLOW)

        val backgroundDrawable = drawBackground(canvas, iconConfig)

        iconConfig.foregroundConfig?.let { foregroundConfig ->
            drawForeground(canvas, foregroundConfig, backgroundDrawable, iconConfig.backgroundConfig.size)
        }

        iconConfig.headerText?.let { headerText ->
            drawText(canvas, headerText, context)
        }

        return bitmap
    }

    private fun createBitmap(context: Context, iconConfig: IconConfig): Bitmap {

        val textWidth = iconConfig.headerText?.let { headerText ->
            textPaint(context).measureText(headerText).toInt()
        } ?: kotlin.run {
            0
        }

        val textSize = iconConfig.headerText?.let {
            textPaint(context).textSize.toInt()
        } ?: kotlin.run {
            0
        }

        val iconWidth = maxOf(textWidth, iconConfig.backgroundConfig.size)
        val iconHeight = if(!iconConfig.headerText.isNullOrEmpty()) iconConfig.backgroundConfig.size + textSize else iconConfig.backgroundConfig.size

        return Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888)
    }

    private fun drawBackground(canvas: Canvas, iconConfig: IconConfig): Drawable {
        val widthMargin = (canvas.width - iconConfig.backgroundConfig.size) / 2

        val backgroundDrawable = iconConfig.backgroundConfig.drawable
        backgroundDrawable.setBounds(widthMargin, canvas.height - iconConfig.backgroundConfig.size,  iconConfig.backgroundConfig.size + widthMargin, canvas.height)
        backgroundDrawable.draw(canvas)

        return backgroundDrawable
    }

    private fun drawForeground(canvas: Canvas, foregroundConfig: DrawableConfig, backgroundDrawable: Drawable, backgroundSize: Int) {
        val margin = (backgroundSize - foregroundConfig.size) / 2

        val foregroundDrawable = foregroundConfig.drawable
        foregroundDrawable.setBounds(backgroundDrawable.bounds.left + margin, backgroundDrawable.bounds.top + margin, backgroundDrawable.bounds.right - margin, backgroundDrawable.bounds.bottom - margin)
        foregroundDrawable.draw(canvas)
    }

    private fun drawText(canvas: Canvas, text: String, context: Context) {
        val paint = textPaint(context)

        val x = canvas.width / 2.0f
        val y = paint.textSize

        canvas.drawText(text, x, y, paint)
    }

    private fun textPaint(context: Context): Paint {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.style = Paint.Style.FILL_AND_STROKE
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.color = TEXT_COLOR
        textPaint.textSize = TEXT_RAW_SIZE * context.resources.displayMetrics.density
        return textPaint
    }
}