package com.example.afetcomms.ui.alert

import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.afetcomms.R
import com.example.afetcomms.alert.SosAlertHelper
import com.example.afetcomms.ui.messages.MessagesActivity

class SosAlertActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        setContentView(R.layout.activity_sos_alert)

        val incoming = intent.getBooleanExtra(EXTRA_INCOMING, true)
        val sender = intent.getStringExtra(EXTRA_SENDER) ?: "?"
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: ""

        findViewById<TextView>(R.id.txtSosTitle).text = if (incoming) {
            getString(R.string.sos_alert_incoming_title)
        } else {
            getString(R.string.sos_alert_sent_title)
        }
        findViewById<TextView>(R.id.txtSosSender).text = if (incoming) {
            getString(R.string.sos_alert_sender, sender)
        } else {
            getString(R.string.sos_alert_sent_subtitle)
        }
        findViewById<TextView>(R.id.txtSosContent).text = content

        findViewById<Button>(R.id.btnSosMessages).setOnClickListener {
            startActivity(android.content.Intent(this, MessagesActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.btnSosDismiss).setOnClickListener { dismissAlert() }
    }

    override fun onDestroy() {
        SosAlertHelper.stopAlerts(applicationContext)
        super.onDestroy()
    }

    private fun dismissAlert() {
        SosAlertHelper.stopAlerts(applicationContext)
        finish()
    }

    companion object {
        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_CONTENT = "extra_content"
        const val EXTRA_INCOMING = "extra_incoming"
    }
}
