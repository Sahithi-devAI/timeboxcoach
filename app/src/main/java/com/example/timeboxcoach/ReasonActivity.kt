package com.example.timeboxcoach

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReasonActivity : Activity() {

    private lateinit var reasonInput: EditText
    private lateinit var submitBtn: Button
    private lateinit var feedbackText: TextView

    private var blockedPackage = "unknown"
    private var allowedWindow = ""
    private var submitted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reason)

        blockedPackage = intent.getStringExtra(AppBlockerService.EXTRA_BLOCKED_PACKAGE) ?: "unknown"
        allowedWindow = intent.getStringExtra(AppBlockerService.EXTRA_ALLOWED_WINDOW) ?: ""

        val appName = BlockRulesManager.getRules(this)
            .find { it.packageName == blockedPackage }?.appName ?: blockedPackage
        findViewById<TextView>(R.id.reasonTitle).text = "$appName is blocked"

        reasonInput = findViewById(R.id.reasonInput)
        submitBtn = findViewById(R.id.submitReason)
        feedbackText = findViewById(R.id.feedbackText)

        submitBtn.setOnClickListener { handleSubmit() }
    }

    private fun handleSubmit() {
        if (submitted) {
            goHome()
            return
        }

        val reason = reasonInput.text.toString().trim()
        if (reason.isEmpty()) {
            feedbackText.setTextColor(0xFFCC0000.toInt())
            showFeedback("Please write something first.")
            return
        }

        ReasonLogger.log(
            this,
            ReasonLog(
                timestamp = System.currentTimeMillis(),
                blockedApp = blockedPackage,
                allowedWindow = allowedWindow,
                actualTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                reason = reason,
                decision = "denied"
            )
        )

        AppBlockerService.temporaryAccessUntil = 0L
        submitted = true
        reasonInput.isEnabled = false
        submitBtn.text = "OK, go home"
        feedbackText.setTextColor(0xFF333333.toInt())
        showFeedback("Noted. Access stays blocked.\nIf this genuinely can't wait, use your computer.")
    }

    private fun showFeedback(message: String) {
        feedbackText.text = message
        feedbackText.visibility = View.VISIBLE
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
    }
}
