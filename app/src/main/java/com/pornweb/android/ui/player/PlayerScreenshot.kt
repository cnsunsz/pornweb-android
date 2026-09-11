package com.pornweb.android.ui.player

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.media3.ui.PlayerView
import java.io.OutputStream

/**
 * Capture current frame from PlayerView (TextureView preferred) and save to gallery.
 */
object PlayerScreenshot {
    fun captureAndSave(context: Context, playerView: PlayerView?, onDone: ((Boolean) -> Unit)? = null) {
        if (playerView == null || playerView.width <= 0 || playerView.height <= 0) {
            Toast.makeText(context, "截图失败", Toast.LENGTH_SHORT).show()
            onDone?.invoke(false)
            return
        }
        val w = playerView.width
        val h = playerView.height
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val surfaceView = playerView.videoSurfaceView

        fun finish(ok: Boolean, bmp: Bitmap?) {
            if (!ok || bmp == null) {
                Toast.makeText(context, "截图失败", Toast.LENGTH_SHORT).show()
                onDone?.invoke(false)
                return
            }
            val saved = saveToGallery(context, bmp)
            bmp.recycle()
            Toast.makeText(
                context,
                if (saved) "已保存到相册" else "截图保存失败",
                Toast.LENGTH_SHORT
            ).show()
            onDone?.invoke(saved)
        }

        when (surfaceView) {
            is TextureView -> {
                val ok = surfaceView.getBitmap(bitmap) != null
                finish(ok, if (ok) bitmap else null)
            }
            else -> {
                // Fallback: copy from activity window region covering the player view.
                val loc = IntArray(2)
                playerView.getLocationInWindow(loc)
                val window = (context as? android.app.Activity)?.window
                if (window == null) {
                    finish(false, null)
                    return
                }
                val rect = android.graphics.Rect(loc[0], loc[1], loc[0] + w, loc[1] + h)
                try {
                    PixelCopy.request(
                        window,
                        rect,
                        bitmap,
                        { result ->
                            Handler(Looper.getMainLooper()).post {
                                finish(result == PixelCopy.SUCCESS, if (result == PixelCopy.SUCCESS) bitmap else null)
                            }
                        },
                        Handler(Looper.getMainLooper())
                    )
                } catch (_: Exception) {
                    finish(false, null)
                }
            }
        }
    }

    private fun saveToGallery(context: Context, bitmap: Bitmap): Boolean {
        val name = "PornWeb_${System.currentTimeMillis()}.jpg"
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PornWeb")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { out: OutputStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)) return false
            } ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
