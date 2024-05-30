import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

class CircleTransform : BitmapTransformation() {
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("circle_transform".toByteArray())
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        // Default marker size in dp
        val defaultSizeInDp = 30

        // Convert dp to pixels
        val density = Resources.getSystem().displayMetrics.density
        val defaultSizeInPx = (defaultSizeInDp * density).toInt()

        // Resize the bitmap
        val resizedBitmap = Bitmap.createScaledBitmap(toTransform, defaultSizeInPx, defaultSizeInPx, false)

        // Create the circular cropped bitmap
        val size = resizedBitmap.width.coerceAtMost(resizedBitmap.height)
        val x = (resizedBitmap.width - size) / 2
        val y = (resizedBitmap.height - size) / 2

        val squared = Bitmap.createBitmap(resizedBitmap, x, y, size, size)
        val result = Bitmap.createBitmap(defaultSizeInPx, defaultSizeInPx, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(result)
        val paint = Paint()
        paint.shader = BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.isAntiAlias = true
        val r = defaultSizeInPx / 2f
        canvas.drawCircle(r, r, r, paint)

        return result
    }
}
