package io.stanc.pogotool.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log
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
        var headerDrawable: Drawable? = null,
        var sizeMod: SizeMod = IconFactory.SizeMod.DEFAULT,
        var headerText: String? = null,
        var footerText: String? = null
    )

    private const val HEADER_TEXT_RAW_SIZE: Float = 10.0f
    private const val FOOTER_TEXT_RAW_SIZE: Float = 10.0f
    private const val TEXT_COLOR = Color.BLACK
    private const val TEXT_BACKGROUND_COLOR = Color.WHITE

    fun bitmap(context: Context, @DrawableRes id: Int, sizeMode: SizeMod = IconFactory.SizeMod.DEFAULT): Bitmap? {

        ContextCompat.getDrawable(context, id)?.let { drawable ->

            val height = scaledLength(drawable.intrinsicHeight, sizeMode)
            val width = scaledLength(drawable.intrinsicWidth, sizeMode)

            drawable.setBounds(0, 0, width, height)
            val bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bm)
            drawable.draw(canvas)

            return bm

        } ?: run {
            return null
        }
    }

    fun bitmap(context: Context, iconConfig: IconConfig): Bitmap {

        val bitmap = createBitmap(context, iconConfig)
        val canvas = Canvas(bitmap)

        // TODO: for debugging only
//        canvas.drawColor(Color.YELLOW)

        val backgroundDrawable = drawBackgroundImage(canvas, iconConfig, context)

        iconConfig.foregroundDrawable?.let { foregroundDrawable ->
            drawForegroundImage(canvas, foregroundDrawable, backgroundDrawable, iconConfig.sizeMod)
        }

        iconConfig.headerDrawable?.let { headerDrawable ->
            drawHeaderImage(canvas, headerDrawable, iconConfig)
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

        val bitmapWidth = bitmapWidth(context, iconConfig)
        val bitmapHeight = bitmapHeight(context, iconConfig)

        return Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    }

    private fun bitmapWidth(context: Context, iconConfig: IconConfig): Int {

        var iconWidth = scaledLength(iconConfig.backgroundDrawable.intrinsicWidth, iconConfig.sizeMod)

        iconConfig.headerText?.let { headerText ->
            iconWidth = maxOf(iconWidth, textPaint(context, HEADER_TEXT_RAW_SIZE).measureText(headerText).toInt())
        }

        iconConfig.footerText?.let { footerText ->
            iconWidth = maxOf(iconWidth, textPaint(context, FOOTER_TEXT_RAW_SIZE).measureText(footerText).toInt())
        }

        iconConfig.headerDrawable?.let { _ ->
            val headerDrawableSize = headerDrawableSize(iconConfig.backgroundDrawable, iconConfig.sizeMod)
            iconWidth = maxOf(iconWidth, headerDrawableSize)
        }

        return iconWidth
    }

    private fun bitmapHeight(context: Context, iconConfig: IconConfig): Int {

        var iconHeight = scaledLength(iconConfig.backgroundDrawable.intrinsicHeight, iconConfig.sizeMod)

        iconConfig.headerText?.let {
            iconHeight += textPaint(context, HEADER_TEXT_RAW_SIZE).textSize.toInt()
        }

        iconConfig.footerText?.let {
            iconHeight += (textPaint(context, FOOTER_TEXT_RAW_SIZE).textSize * 1.1).toInt()
        }

        iconConfig.headerDrawable?.let {
            val headerDrawableSize = headerDrawableSize(iconConfig.backgroundDrawable, iconConfig.sizeMod)
            iconHeight += headerDrawableSize
        }

        return iconHeight
    }

    private fun drawBackgroundImage(canvas: Canvas, iconConfig: IconConfig, context: Context): Drawable {
        val marginHorizontal = (canvas.width - scaledLength(iconConfig.backgroundDrawable.intrinsicWidth, iconConfig.sizeMod)) / 2

        var marginTop = 0
        iconConfig.headerText?.let {
            marginTop += textPaint(context, HEADER_TEXT_RAW_SIZE).textSize.toInt()
        }
        iconConfig.headerDrawable?.let {
            marginTop += headerDrawableSize(iconConfig.backgroundDrawable, iconConfig.sizeMod)
        }

        val marginBottom = if (iconConfig.footerText.isNullOrEmpty()) 0 else textPaint(context, FOOTER_TEXT_RAW_SIZE).textSize.toInt()

        val backgroundDrawable = iconConfig.backgroundDrawable

        val left = marginHorizontal
        val top = marginTop
        val right = scaledLength(iconConfig.backgroundDrawable.intrinsicWidth, iconConfig.sizeMod) + marginHorizontal
        val bottom = canvas.height - marginBottom

        backgroundDrawable.setBounds(left, top,  right, bottom)
        backgroundDrawable.draw(canvas)

        return backgroundDrawable
    }

    private fun drawForegroundImage(canvas: Canvas, foregroundDrawable: Drawable, backgroundDrawable: Drawable, sizeMod: SizeMod) {
        val marginHorizontal = scaledLength(backgroundDrawable.intrinsicWidth, sizeMod) / 4
        val marginVertically = scaledLength(backgroundDrawable.intrinsicHeight, sizeMod) / 4

        val left = backgroundDrawable.bounds.left + marginHorizontal
        val top = backgroundDrawable.bounds.top + marginVertically
        val right = backgroundDrawable.bounds.right - marginHorizontal
        val bottom = backgroundDrawable.bounds.bottom - marginVertically

        foregroundDrawable.setBounds(left, top, right, bottom)
        foregroundDrawable.draw(canvas)
    }

    private fun drawHeaderImage(canvas: Canvas, headerDrawable: Drawable, iconConfig: IconConfig) {

        val headerSize = headerDrawableSize(iconConfig.backgroundDrawable, iconConfig.sizeMod)

        val left = (canvas.width - headerSize) / 2
        val top = 0
        val right = headerSize + (canvas.width - headerSize) / 2
        val bottom = headerSize

        headerDrawable.setBounds(left, top, right, bottom)
        headerDrawable.draw(canvas)
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
        val y = canvas.height - paint.textSize / 6.0

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        fillPaint.color = TEXT_BACKGROUND_COLOR
        canvas.drawRect(0.0f, canvas.height - paint.textSize, canvas.width.toFloat(), canvas.height.toFloat(), fillPaint)

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

    private fun headerDrawableSize(backgroundDrawable: Drawable, sizeMod: SizeMod): Int {
        return scaledLength(backgroundDrawable.intrinsicHeight, sizeMod)
    }
}