package app.example.landmarkremarkapplication.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import app.example.landmarkremarkapplication.R
import app.example.landmarkremarkapplication.constrants.AppConstants
import app.example.landmarkremarkapplication.databinding.ActivityLoginBinding
import app.example.landmarkremarkapplication.model.entity.UserModel
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var onTapClient: SignInClient? = null
    private lateinit var signInRequest: BeginSignInRequest
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        onTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.default_web_client_id))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()
        initData()
    }

    private fun initData() {
        binding.btnSign.setOnClickListener {
            if (checkData()) {
                auth.signInWithEmailAndPassword(
                    binding.edtEmail.text.toString().trim(),
                    binding.edtPassword.text.toString().trim()
                ).addOnSuccessListener {
                    AppConstants.setToast(
                        this,
                        getString(R.string.success),
                        getString(R.string.well_come_to_my_app),
                        1
                    )
                    val intent = Intent(this, MapsActivity::class.java)
                    startActivity(intent)
                }.addOnFailureListener {
                    AppConstants.setToast(
                        this,
                        getString(R.string.sign_fail),
                        getString(R.string.please_check_you_email_and_password),
                        2
                    )
                }
            } else {
                AppConstants.setToast(
                    this,
                    getString(R.string.insufficient_data),
                    getString(R.string.enter_full_data),
                    6
                )
            }
        }
        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Xử lý sự kiện khi người dùng đăng nhập bằng tài khoản Google.
     * Sự kiện này được kích hoạt khi người dùng nhấn vào nút đăng nhập bằng Google trên giao diện.
     * Phương thức này sẽ gọi phương thức signingGoogle() trong một coroutine để thực hiện đăng nhập.
     */
    fun signingGoogle(view: View) {
        CoroutineScope(Dispatchers.Main).launch {
            signingGoogle()
        }
    }

    /**
     * Thực hiện quá trình đăng nhập bằng tài khoản Google.
     * Phương thức này được gọi từ phương thức signingGoogle() và sẽ chờ kết quả trả về từ Google Sign-In API.
     */
    private suspend fun signingGoogle() {
        val result = onTapClient?.beginSignIn(signInRequest)?.await()
        val intentSenderRequest = IntentSenderRequest.Builder(result!!.pendingIntent).build()
        activityResultLauncher.launch(intentSenderRequest)
    }

    /**
     * Xử lý kết quả trả về từ quá trình đăng nhập bằng tài khoản Google.
     * Phương thức này được gọi khi kết quả trả về từ hoạt động đăng nhập.
     * Nếu đăng nhập thành công, nó sẽ lưu thông tin người dùng vào cơ sở dữ liệu Firebase và chuyển đến MapsActivity.
     */
    private val activityResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                try {
                    val credential = onTapClient?.getSignInCredentialFromIntent(result.data)
                    val idToken = credential?.googleIdToken

                    idToken?.let {
                        val firebaseCredential = GoogleAuthProvider.getCredential(it, null)
                        auth.signInWithCredential(firebaseCredential)
                            .addOnSuccessListener { authResult ->
                                val user = authResult.user
                                user?.let { firebaseUser ->
                                    // Hiển thị thông báo đăng nhập thành công
                                    AppConstants.setToast(
                                        this,
                                        getString(R.string.sign_success),
                                        getString(R.string.well_come_to_my_app),
                                        1
                                    )

                                    val firebaseDatabase =
                                        FirebaseDatabase.getInstance().getReference("Users")
                                    firebaseDatabase.child(firebaseUser.uid).get()
                                        .addOnSuccessListener { dataSnapshot ->
                                            if (!dataSnapshot.exists()) {
                                                // Tạo mô hình người dùng mới
                                                val userSignUp = UserModel(
                                                    firebaseUser.uid,
                                                    firebaseUser.displayName,
                                                    firebaseUser.email,
                                                    "",
                                                    firebaseUser.photoUrl.toString()
                                                )
                                                // Lưu thông tin người dùng vào cơ sở dữ liệu
                                                firebaseDatabase.child(firebaseUser.uid)
                                                    .setValue(userSignUp)
                                            }
                                            // Chuyển đến MapsActivity sau khi đăng nhập
                                            startActivity(Intent(this, MapsActivity::class.java))
                                        }.addOnFailureListener {
                                        // Hiển thị thông báo nếu có lỗi xảy ra
                                        AppConstants.setToast(
                                            this,
                                            getString(R.string.sign_fail),
                                            it.message.toString(),
                                            2
                                        )
                                    }
                                }
                            }.addOnFailureListener {
                            // Hiển thị thông báo nếu có lỗi xảy ra
                            AppConstants.setToast(
                                this,
                                getString(R.string.sign_fail),
                                it.message.toString(),
                                2
                            )
                        }
                    }

                } catch (e: ApiException) {
                    e.printStackTrace()
                }
            }
        }

    /**
     * Kiểm tra xem dữ liệu nhập từ người dùng có hợp lệ không.
     * Phương thức này kiểm tra xem các trường email và password có trống không.
     * Nếu một trong hai trường rỗng, nó sẽ hiển thị thông báo lỗi tương ứng và trả về false.
     * Nếu cả hai trường đều có dữ liệu, nó sẽ trả về true.
     */
    private fun checkData(): Boolean {
        var isValid = true
        if (binding.edtEmail.text.toString().isEmpty()) {
            binding.edtEmail.error = getString(R.string.please_enter_email)
            isValid = false
        }

        if (binding.edtPassword.text.toString().isEmpty()) {
            binding.edtPassword.error = getString(R.string.please_not_empty)
            isValid = false
        }
        return isValid
    }


}