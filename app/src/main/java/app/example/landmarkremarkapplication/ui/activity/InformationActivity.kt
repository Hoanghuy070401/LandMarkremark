package app.example.landmarkremarkapplication.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.example.landmarkremarkapplication.R
import app.example.landmarkremarkapplication.constrants.AppConstants
import app.example.landmarkremarkapplication.databinding.ActivityUpdateInformationBinding
import app.example.landmarkremarkapplication.model.entity.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class InformationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateInformationBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var uid: String
    private var user = UserModel()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")
        if (auth.currentUser != null) {
            uid = auth.currentUser!!.uid
            showUser()
        }
        binding.btnSignUot.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this@InformationActivity, LoginActivity::class.java))
        }

    }

    // Lấy thông tin người dùng từ Firebase và hiển thị ảnh đại diện của họ
    private fun showUser() {
        database.child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                user = snapshot.getValue(UserModel::class.java)!!
                user.photo?.let { AppConstants.loadPhoto(binding.crAvatar, it) }
                binding.tvName.text = user.userName
                binding.tvEmail.text = user.email
            }

            override fun onCancelled(error: DatabaseError) {
                AppConstants.setToast(
                    this@InformationActivity,
                    getString(R.string.fails),
                    error.message,
                    2
                )
            }
        })
    }
}