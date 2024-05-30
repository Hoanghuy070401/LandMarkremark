package app.example.landmarkremarkapplication.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import app.example.landmarkremarkapplication.R
import app.example.landmarkremarkapplication.constrants.AppConstants
import app.example.landmarkremarkapplication.databinding.ActivitySignupBinding
import app.example.landmarkremarkapplication.model.entity.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseDatabase: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var uri: Uri
    private var checkPhoto = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        storageReference = FirebaseStorage.getInstance().getReference("Images")
        firebaseDatabase = FirebaseDatabase.getInstance().getReference("Users")
        auth = FirebaseAuth.getInstance()
        val activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) {
            uri = it!!
            binding.crAvatar.setImageURI(it)
            checkPhoto = true

        }

        binding.crAvatar.setImageResource(R.drawable.ic_user_default)
        binding.tvBackSign.setOnClickListener {
            finish()
        }
        binding.llAvatar.setOnClickListener {
            activityResultLauncher.launch("image/*")
        }
        binding.btnSignUp.setOnClickListener {
            if (checkPhoto) {
                checkData()
            } else {
                AppConstants.setToast(
                    this,
                    getString(R.string.insufficient_data),
                    getString(R.string.please_choose_avatar),
                    6
                )
            }

        }
    }

    /**
     * Kiểm tra thông tin nhập từ người dùng khi đăng ký.
     * Phương thức này kiểm tra xem các trường tên, email, mật khẩu và xác nhận mật khẩu có hợp lệ không.
     * Nếu một trong các trường rỗng, nó sẽ hiển thị thông báo lỗi tương ứng.
     * Nếu trường xác nhận mật khẩu không khớp với mật khẩu đã nhập, nó cũng sẽ hiển thị thông báo lỗi.
     * Nếu tất cả các trường đều hợp lệ, nó sẽ gọi phương thức saveData() để lưu thông tin người dùng.
     */
    private fun checkData() {
        var isValid = true

        if (binding.edtName.text.toString().isEmpty()) {
            binding.edtName.error = getString(R.string.please_enter_name)
            isValid = false
        }

        if (binding.edtEmail.text.toString().isEmpty()) {
            binding.edtEmail.error = getString(R.string.please_enter_email)
            isValid = false
        }

        if (binding.edtPassword.text.toString().isEmpty()) {
            binding.edtPassword.error = getString(R.string.please_not_empty)
            isValid = false
        }

        if (binding.edtPasswordAgain.text.toString().isEmpty()) {
            binding.edtPasswordAgain.error = getString(R.string.please_not_empty)
            isValid = false
        } else if (binding.edtPasswordAgain.text.toString() != binding.edtPassword.text.toString()) {
            binding.edtPasswordAgain.error = getString(R.string.password_not_like)
            isValid = false
        }

        if (isValid) {
            saveData()
        } else {
            // Hiển thị thông báo nếu dữ liệu nhập không hợp lệ
            AppConstants.setToast(
                this,
                getString(R.string.insufficient_data),
                getString(R.string.enter_full_data),
                6
            )
        }
    }

    /**
     * Lưu thông tin người dùng vào cơ sở dữ liệu khi đăng ký.
     * Phương thức này sẽ lấy dữ liệu từ các trường nhập và tải ảnh đại diện của người dùng lên lưu trữ Firebase.
     * Sau đó, nó tạo một mô hình người dùng và lưu vào cơ sở dữ liệu Firebase.
     * Nếu quá trình này thành công, nó sẽ chuyển người dùng đến MapsActivity.
     * Nếu có lỗi xảy ra, nó sẽ hiển thị thông báo lỗi tương ứng.
     */
    private fun saveData() {
        val email = binding.edtEmail.text.toString().trim()
        val password = binding.edtPassword.text.toString().trim()
        val name = binding.edtName.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            // Hiển thị thông báo nếu dữ liệu không đầy đủ
            AppConstants.setToast(
                this,
                getString(R.string.fail_sign_up),
                getString(R.string.enter_full_data),
                2
            )
            return
        }

        lifecycleScope.launch {
            try {
                // Tạo người dùng mới với email và mật khẩu
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = authResult.user!!.uid

                // Tải ảnh đại diện của người dùng lên lưu trữ
                val uploadTask = storageReference.child(uid).putFile(uri).await()
                val downloadUrl = uploadTask.metadata!!.reference!!.downloadUrl.await()
                val photoUrl = downloadUrl.toString()

                // Tạo mô hình người dùng
                val user = UserModel(
                    uid,
                    name,
                    email,
                    password,
                    photoUrl
                )

                // Lưu thông tin người dùng vào cơ sở dữ liệu
                firebaseDatabase.child(uid).setValue(user).await()

                // Hiển thị thông báo đăng ký thành công và chuyển đến MapsActivity
                AppConstants.setToast(
                    this@SignupActivity,
                    getString(R.string.success),
                    getString(R.string.sign_up_success),
                    1
                )
                startActivity(Intent(this@SignupActivity, MapsActivity::class.java))
            } catch (e: Exception) {
                // Hiển thị thông báo nếu có lỗi xảy ra trong quá trình đăng ký
                AppConstants.setToast(
                    this@SignupActivity,
                    getString(R.string.fail_sign_up),
                    e.message.toString(),
                    2
                )
            }
        }
    }


}