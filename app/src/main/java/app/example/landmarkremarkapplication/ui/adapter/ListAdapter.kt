package app.example.landmarkremarkapplication.ui.adapter


import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.recyclerview.widget.RecyclerView
import app.example.landmarkremarkapplication.constrants.AppConstants
import app.example.landmarkremarkapplication.databinding.ItemLayoutNotesBinding
import app.example.landmarkremarkapplication.model.entity.NotesModel


class ListAdapter(private val notes: ArrayList<NotesModel>) :
    RecyclerView.Adapter<ListAdapter.NotesViewHolder>() {
    // Interface để xử lý sự kiện khi một mục trong danh sách được nhấp
    var clickItem: OnclickItem? = null

    /**
     * ViewHolder cho mỗi mục ghi chú trong danh sách.
     * @param binding Dữ liệu giao diện cho mục ghi chú.
     */
    class NotesViewHolder(val binding: ItemLayoutNotesBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
        // Tạo ViewHolder từ layout XML của mục ghi chú
        val binding =
            ItemLayoutNotesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) {
        // Gắn dữ liệu của ghi chú vào ViewHolder
        val note = notes[position]
        with(holder.binding) {
            AppConstants.loadPhoto(crAvatar, note.photo!!)
            tvUserName.text = note.userName
            tvAddress.text = note.address
            tvNotes.text = note.note
        }
        // Xử lý sự kiện khi một mục được nhấp
        holder.itemView.setOnClickListener {
            clickItem?.onClick(position)
        }
    }

    /**
     * Xóa toàn bộ dữ liệu trong danh sách và cập nhật RecyclerView.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun clearData() {
        notes.clear()
        notifyDataSetChanged()
    }

    /**
     * Thêm danh sách ghi chú mới vào danh sách hiện tại và cập nhật RecyclerView.
     * @param listNotes Danh sách ghi chú mới.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun addData(listNotes: ArrayList<NotesModel>) {
        notes.addAll(listNotes)
        notifyDataSetChanged()
    }

    override fun getItemCount() = notes.size

    /**
     * Interface để xử lý sự kiện khi một mục trong danh sách được nhấp.
     */
    interface OnclickItem {
        fun onClick(position: Int)
    }
}
