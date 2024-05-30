package app.example.landmarkremarkapplication.widget
import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

class MaxHeightRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private var maxHeight: Int = 0

    fun setMaxHeight(maxHeight: Int) {
        this.maxHeight = maxHeight
        requestLayout() // Request layout to apply changes
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        var heightSpecModified = heightSpec
        if (maxHeight > 0) {
            val maxHeightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
            heightSpecModified = if (MeasureSpec.getSize(heightSpec) > maxHeight) {
                maxHeightSpec
            } else {
                heightSpec
            }
        }
        super.onMeasure(widthSpec, heightSpecModified)
    }
}
