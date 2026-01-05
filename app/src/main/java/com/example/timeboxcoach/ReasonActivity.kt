package com.example.timeboxcoach

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class ReasonActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reason)

        val reasonInput = findViewById<EditText>(R.id.reasonInput)
        val submitBtn = findViewById<Button>(R.id.submitReason)

        submitBtn.setOnClickListener {
            val reason = reasonInput.text.toString().trim()

            AppBlockerService.lastReason = reason

            finish()
        }
    }
}
