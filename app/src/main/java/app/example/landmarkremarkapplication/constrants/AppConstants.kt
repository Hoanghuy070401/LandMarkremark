package app.example.landmarkremarkapplication.constrants

import android.app.Activity
import android.content.Context
import android.media.Image
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import app.example.landmarkremarkapplication.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.text.MessageFormat

object AppConstants {
    const val UID ="UID"
    /** type: 1 thông báo thành công
     *        2 thông báo cảnh báo lỗi phần mềm
     *        3 thông báo xóa
     *        4 thông báo thông tin
     *        5 thông báo cảnh báo kết nối mạng
     *        6 thông báo cảnh báo nguy cơ
     */
    fun setToast(context: Activity,title:String,message:String,type:Int){
        when(type){
            1 -> MotionToast.darkColorToast(context,
                title,
                message,
                MotionToastStyle.SUCCESS,
                MotionToast.GRAVITY_BOTTOM,
                MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(context, R.font.roboto))
            2-> MotionToast.darkColorToast(context,
                title,
                message,
                MotionToastStyle.ERROR,
                MotionToast.GRAVITY_BOTTOM,
                MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(context, R.font.roboto))
            3-> MotionToast.darkColorToast(context,
                title,
                message,
                MotionToastStyle.DELETE,
                MotionToast.GRAVITY_BOTTOM,
                MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(context, R.font.roboto))
            4-> MotionToast.darkColorToast(context,
                title,
                message,
                MotionToastStyle.INFO,
                MotionToast.GRAVITY_BOTTOM,
                MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(context, R.font.roboto))
            5-> MotionToast.darkColorToast(context,
                title,
                message,
                MotionToastStyle.NO_INTERNET,
                MotionToast.GRAVITY_BOTTOM,
                MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(context, R.font.roboto))
            6-> MotionToast.darkColorToast(context,
                title,
                message,
                MotionToastStyle.WARNING,
                MotionToast.GRAVITY_BOTTOM,
                MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(context, R.font.roboto))
        }

    }
    fun loadPhoto(image: ImageView,url:String){
        Glide.with(image)
            .asBitmap()
            .load(url
            )
            .diskCacheStrategy(DiskCacheStrategy.ALL).centerCrop().apply(
                RequestOptions().placeholder(R.drawable.ic_user_default)
                    .error(R.drawable.ic_user_default)
            ).into(image)
    }
    fun postDelayed(delayMillis: Long, action: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            action()
        }, delayMillis)
    }
}