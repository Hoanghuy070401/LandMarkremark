package app.example.landmarkremarkapplication.ui.dialog


import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import app.example.landmarkremarkapplication.databinding.LayoutAddLocationBinding

class InputDialogNote(context: Context) : Dialog(context) {

    private lateinit var binding: LayoutAddLocationBinding
    private var onActionDone: OnActionDone? = null

    fun setOnActionDone(listener: OnActionDone) {
        this.onActionDone = listener
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = LayoutAddLocationBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        // Đảm bảo Dialog nằm giữa màn hình với chiều rộng bằng 3/4 chiều rộng màn hình
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = (displayMetrics.widthPixels * 0.75).toInt()

        window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        val params: WindowManager.LayoutParams? = window?.attributes
        params?.gravity = android.view.Gravity.CENTER
        window?.attributes = params




        binding.btnConfirm.setOnClickListener {
            val notes = binding.edtNote.text.toString().trim()
            onActionDone?.onActionDone(true,notes)
        }
        binding.btnCancel.setOnClickListener {
            onActionDone?.onActionDone(false,"")
        }


    }
    interface OnActionDone {
        fun onActionDone(isConfirm:Boolean,note: String)
    }
}