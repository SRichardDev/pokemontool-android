package io.stanc.pogoradar.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream

object Asset {

    private val TAG = javaClass.name

    fun imageDrawable(context: Context, assetDir: String, imageName: String, imageType: String): Drawable? {

        return try {
            val filePath = if (assetDir.isEmpty()) "$imageName.$imageType" else "$assetDir/$imageName.$imageType"
            val inputStream = context.assets.open(filePath)
            Drawable.createFromStream(inputStream, null)

        } catch (ex: IOException) {
            Log.e(TAG, ex.toString())
            null
        }
    }

    fun fileJsonObject(context: Context, assetDir: String, fileName: String): JSONObject? {
        return JSONObject(fileText(context, assetDir, fileName, "json"))
    }

    fun fileJsonArray(context: Context, assetDir: String, fileName: String): JSONArray? {
        return JSONArray(fileText(context, assetDir, fileName, "json"))
    }

    fun fileText(context: Context, assetDir: String, fileName: String, fileType: String): String? {

        return try {
            val filePath = if (assetDir.isEmpty()) "$fileName.$fileType" else "$assetDir/$fileName.$fileType"
            val inputStream: InputStream = context.assets.open(filePath)
            inputStream.bufferedReader().use{it.readText()}

        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }
}