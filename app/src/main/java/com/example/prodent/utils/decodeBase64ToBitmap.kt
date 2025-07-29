package com.example.prodent.utils

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.InputStream
import android.graphics.BitmapFactory

fun decodeBase64ToBitmap(base64String: String): Bitmap? {
    return try {
        val decodedString: ByteArray = Base64.decode(base64String, Base64.DEFAULT)
        val inputStream: InputStream = ByteArrayInputStream(decodedString)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}