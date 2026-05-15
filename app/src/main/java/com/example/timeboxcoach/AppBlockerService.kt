package com.example.timeboxcoach

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.TextView
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val SELF_PACKAGE = "com.example.timeboxcoach"

class AppBlockerService : AccessibilityService() {

    companion object {
        const val TAG = "TimeboxCoach"
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_ALLOWED_WINDOW = "extra_allowed_window"

        // Set by ReasonActivity after evaluation
        var temporaryAccessUntil: Long = 0L
    }

    private var overlayView: View? = null
    private var activeBlockedPackage: String? = null
    private lateinit var windowManager: WindowManager
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == SELF_PACKAGE) return

        val rules = BlockRulesManager.getRules(this)
        val matchedRule = rules.find { it.packageName == packageName }

        if (matchedRule != null) {
            if (isWithinAllowedTime(matchedRule)) {
                removeOverlay()
                return
            }

            if (System.currentTimeMillis() < temporaryAccessUntil) {
                removeOverlay()
                Log.d(TAG, "${matchedRule.appName}: temporary access active")
                return
            }

            showOverlay(matchedRule)
        } else if (overlayView != null) {
            removeOverlay()
        }
    }

    private fun isWithinAllowedTime(rule: BlockRule): Boolean {
        val now = LocalTime.now()
        val start = LocalTime.parse(rule.allowedStart, timeFormatter)
        val end = LocalTime.parse(rule.allowedEnd, timeFormatter)
        return !now.isBefore(start) && now.isBefore(end)
    }

    private fun showOverlay(rule: BlockRule) {
        if (overlayView != null && activeBlockedPackage == rule.packageName) return
        if (overlayView != null) removeOverlay()

        activeBlockedPackage = rule.packageName
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.block_overlay, null)

        overlayView!!.findViewById<TextView>(R.id.blockTitle).text = "${rule.appName} is blocked"
        overlayView!!.findViewById<TextView>(R.id.blockSubtitle).text =
            "Allowed between ${rule.allowedStart}–${rule.allowedEnd}.\n" +
            "Override only if this has real impact on your work, health, or relationships."

        overlayView!!.findViewById<Button>(R.id.explainButton).setOnClickListener {
            removeOverlay()
            val intent = Intent(this, ReasonActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_BLOCKED_PACKAGE, rule.packageName)
                putExtra(EXTRA_ALLOWED_WINDOW, "${rule.allowedStart}–${rule.allowedEnd}")
            }
            startActivity(intent)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        try {
            windowManager.addView(overlayView, params)
            Log.d(TAG, "${rule.appName} blocked — overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay", e)
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay", e)
            }
            overlayView = null
        }
    }

    override fun onInterrupt() {}
}
