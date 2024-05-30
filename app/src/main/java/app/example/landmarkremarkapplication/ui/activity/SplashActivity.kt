package app.example.landmarkremarkapplication.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat.postDelayed
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.example.landmarkremarkapplication.R
import app.example.landmarkremarkapplication.constrants.AppConstants
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        goToActivity()

    }

    private fun goToActivity() {
        val user= FirebaseAuth.getInstance().currentUser
        AppConstants.postDelayed(1500) {
            if (user!=null) {
                try {
                    val intent = Intent(this@SplashActivity, MapsActivity::class.java)
                    startActivity(intent)
                    finish()
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                }
            } else {
                try {
                    val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
    }

}