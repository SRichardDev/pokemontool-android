package io.stanc.pogotool.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

object IconFactory {
    private val TAG = javaClass.name

    enum class SizeMod {
        DEFAULT,
        BIG,
        LARGE
    }

    data class IconConfig (
        val backgroundDrawable: Drawable,
        var foregroundDrawable: Drawable? = null,
        var sizeMod: SizeMod = IconFactory.SizeMod.DEFAULT,
        var headerText: String? = null,
        var footerText: String? = null
    )

    private const val HEADER_TEXT_RAW_SIZE: Float = 10.0f
    private const val FOOTER_TEXT_RAW_SIZE: Float = 10.0f
    private const val TEXT_COLOR = Color.BLACK

    fun bitmap(context: Context, @DrawableRes id: Int, sizeMode: SizeMod = IconFactory.SizeMod.DEFAULT): Bitmap? {

        ContextCompat.getDrawable(context, id)?.let { drawable ->

            val height = scaledLength(drawable.intrinsicHeight, sizeMode)
            val width = scaledLength(drawable.intrinsicWidth, sizeMode)

            drawable.setBounds(0, 0, width, height)
            val bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bm)
            drawable.draw(canvas)

            return bm

        } ?: kotlin.run {
            return null
        }
    }

    fun bitmap(context: Context, iconConfig: IconConfig): Bitmap {

        val bitmap = createBitmap(context, iconConfig)
        val canvas = Canvas(bitmap)

        // TODO: for debugging only
//        canvas.drawColor(Color.YELLOW)

        val backgroundDrawable = drawBackground(canvas, iconConfig, context)

        iconConfig.foregroundDrawable?.let { foregroundDrawable ->
            drawForeground(canvas, foregroundDrawable, backgroundDrawable, iconConfig.sizeMod)
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

        var iconWidth = scaledLength(iconConfig.backgroundDrawable.intrinsicWidth, iconConfig.sizeMod)
        iconConfig.headerText?.let { headerText ->
            iconWidth = maxOf(iconWidth, textPaint(context, HEADER_TEXT_RAW_SIZE).measureText(headerText).toInt())
        }
        iconConfig.footerText?.let { footerText ->
            iconWidth = maxOf(iconWidth, textPaint(context, FOOTER_TEXT_RAW_SIZE).measureText(footerText).toInt())
        }

        var iconHeight = scaledLength(iconConfig.backgroundDrawable.intrinsicHeight, iconConfig.sizeMod)
        iconConfig.headerText?.let {
            iconHeight += textPaint(context, HEADER_TEXT_RAW_SIZE).textSize.toInt()
        }
        iconConfig.footerText?.let {
            iconHeight += textPaint(context, FOOTER_TEXT_RAW_SIZE).textSize.toInt()
        }

        return Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888)
    }

    private fun drawBackground(canvas: Canvas, iconConfig: IconConfig, context: Context): Drawable {
        val marginHorizontal = (canvas.width - scaledLength(iconConfig.backgroundDrawable.intrinsicWidth, iconConfig.sizeMod)) / 2

        val marginTop = if (iconConfig.headerText.isNullOrEmpty()) 0 else textPaint(context, HEADER_TEXT_RAW_SIZE).textSize.toInt()
        val marginBottom = if (iconConfig.footerText.isNullOrEmpty()) 0 else textPaint(context, FOOTER_TEXT_RAW_SIZE).textSize.toInt()

        val backgroundDrawable = iconConfig.backgroundDrawable
        backgroundDrawable.setBounds(marginHorizontal, marginTop,  scaledLength(iconConfig.backgroundDrawable.intrinsicWidth, iconConfig.sizeMod) + marginHorizontal, canvas.height - marginBottom)
        backgroundDrawable.draw(canvas)

        return backgroundDrawable
    }

    private fun drawForeground(canvas: Canvas, foregroundDrawable: Drawable, backgroundDrawable: Drawable, sizeMod: SizeMod) {
        val marginHorizontal = (scaledLength(backgroundDrawable.intrinsicWidth, sizeMod) - scaledLength(foregroundDrawable.intrinsicWidth, sizeMod)) / 2
        val marginVertically = (scaledLength(backgroundDrawable.intrinsicHeight, sizeMod) - scaledLength(foregroundDrawable.intrinsicHeight, sizeMod)) / 2

        foregroundDrawable.setBounds(backgroundDrawable.bounds.left + marginHorizontal, backgroundDrawable.bounds.top + marginVertically, backgroundDrawable.bounds.right - marginHorizontal, backgroundDrawable.bounds.bottom - marginVertically)
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

    private fun scaledLength(length: Int, sizeMode: SizeMod): Int {
        val scaleFactor = when(sizeMode) {
            IconFactory.SizeMod.DEFAULT -> 1.0f
            IconFactory.SizeMod.BIG -> 1.5f
            IconFactory.SizeMod.LARGE -> 3.0f
        }
        return (length * scaleFactor).toInt()
    }
}