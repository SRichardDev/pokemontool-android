package io.stanc.pogotool.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable

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
        var headerText: String? = null,
        var footerText: String? = null
    )

    private const val HEADER_TEXT_RAW_SIZE: Float = 10.0f
    private const val FOOTER_TEXT_RAW_SIZE: Float = 10.0f
    private const val TEXT_COLOR = Color.BLACK

    fun bitmap(context: Context, iconConfig: IconConfig): Bitmap {

        val bitmap = createBitmap(context, iconConfig)
        val canvas = Canvas(bitmap)

        // TODO: for debugging
//        canvas.drawColor(Color.YELLOW)

        val backgroundDrawable = drawBackground(canvas, iconConfig, context)

        iconConfig.foregroundConfig?.let { foregroundConfig ->
            drawForeground(canvas, foregroundConfig, backgroundDrawable, iconConfig.backgroundConfig.size)
        }

        iconConfig.headerText?.let { headerText ->
            drawHeaderText(canvas, headerText, context)
        }

        iconConfig.footerText?.let { footerText ->
            drawFooterText(canvas, footerText, context)
        }

        return bitmap
    }

    private fun createBitmap(context: Context, iconConfig: IconConfig): Bitmap {

        var iconWidth = iconConfig.backgroundConfig.size
        iconConfig.headerText?.let { headerText ->
            iconWidth = maxOf(iconWidth, textPaint(context, HEADER_TEXT_RAW_SIZE).measureText(headerText).toInt())
        }
        iconConfig.footerText?.let { footerText ->
            iconWidth = maxOf(iconWidth, textPaint(context, FOOTER_TEXT_RAW_SIZE).measureText(footerText).toInt())
        }

        var iconHeight = iconConfig.backgroundConfig.size
        iconConfig.headerText?.let {
            iconHeight += textPaint(context, HEADER_TEXT_RAW_SIZE).textSize.toInt()
        }
        iconConfig.footerText?.let {
            iconHeight += textPaint(context, FOOTER_TEXT_RAW_SIZE).textSize.toInt()
        }

        return Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888)
    }

    private fun drawBackground(canvas: Canvas, iconConfig: IconConfig, context: Context): Drawable {
        val marginHorizontal = (canvas.width - iconConfig.backgroundConfig.size) / 2

        val marginTop = if (iconConfig.headerText.isNullOrEmpty()) 0 else textPaint(context, HEADER_TEXT_RAW_SIZE).textSize.toInt()
        val marginBottom = if (iconConfig.footerText.isNullOrEmpty()) 0 else textPaint(context, FOOTER_TEXT_RAW_SIZE).textSize.toInt()

        val backgroundDrawable = iconConfig.backgroundConfig.drawable
        backgroundDrawable.setBounds(marginHorizontal, marginTop,  iconConfig.backgroundConfig.size + marginHorizontal, canvas.height - marginBottom)
        backgroundDrawable.draw(canvas)

        return backgroundDrawable
    }

    private fun drawForeground(canvas: Canvas, foregroundConfig: DrawableConfig, backgroundDrawable: Drawable, backgroundSize: Int) {
        val margin = (backgroundSize - foregroundConfig.size) / 2

        val foregroundDrawable = foregroundConfig.drawable
        foregroundDrawable.setBounds(backgroundDrawable.bounds.left + margin, backgroundDrawable.bounds.top + margin, backgroundDrawable.bounds.right - margin, backgroundDrawable.bounds.bottom - margin)
        foregroundDrawable.draw(canvas)
    }

    private fun drawHeaderText(canvas: Canvas, headerText: String, context: Context) {
        val paint = textPaint(context, HEADER_TEXT_RAW_SIZE)

        val x = canvas.width / 2.0f
        val y = paint.textSize

        canvas.drawText(headerText, x, y, paint)
    }

    private fun drawFooterText(canvas: Canvas, subText: String, context: Context) {
        val paint = textPaint(context, FOOTER_TEXT_RAW_SIZE)

        val x = canvas.width / 2.0f
        val y = canvas.height - paint.textSize / 4.0

        canvas.drawText(subText, x, y.toFloat(), paint)
    }

    private fun textPaint(context: Context, textRawSize: Float): Paint {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.style = Paint.Style.FILL_AND_STROKE
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.color = TEXT_COLOR
        textPaint.textSize = textRawSize * context.resources.displayMetrics.density
        return textPaint
    }
}