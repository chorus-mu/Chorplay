package io.github.pushkar.chorplay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class TvActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val forwardedIntent = Intent(intent).apply {
            setClass(this@TvActivity, MainActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(forwardedIntent)
        finish()
    }
}
