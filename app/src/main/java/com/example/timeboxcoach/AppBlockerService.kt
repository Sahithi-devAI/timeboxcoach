package com.example.timeboxcoach

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import java.time.LocalTime
import android.view.KeyEvent
import android.widget.EditText
import android.widget.Button
import android.content.Intent



private val BLOCKED_PACKAGE = "com.instagram.android"
private val SELF_PACKAGE = "com.example.timeboxcoach"
private var isBlockingActive = false
private val ALLOWED_START = LocalTime.of(17,0)  // 5:00 PM
private val ALLOWED_END = LocalTime.of(17, 30)   // 5:30 PM


class AppBlockerService : AccessibilityService() {

    private var overlayView: View? = null
    private lateinit var windowManager: WindowManager
    private fun isWithinAllowedTime(): Boolean {
        val now = LocalTime.now()
        return !now.isBefore(ALLOWED_START) && now.isBefore(ALLOWED_END)
    }

    companion object {
    var lastReason: String? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return
    if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

    val packageName = event.packageName?.toString() ?: return
    Log.d("TimeboxCoach", "Foreground app: $packageName")



    // Ignore our own overlay window
    if (packageName == SELF_PACKAGE) {
        return
    }

    if (packageName == BLOCKED_PACKAGE) {
    if (isWithinAllowedTime()) {
        // Allowed window → ensure no overlay
        isBlockingActive = false
        removeOverlay()
        Log.d("TimeboxCoach", "Instagram allowed (within timebox)")
    } else {
        // Outside window → block
        isBlockingActive = true
        showOverlay()
        Log.d("TimeboxCoach", "Instagram blocked (outside timebox)")
    }
    return

    lastReason?.let { reason ->
    Log.d("TimeboxCoach", "User reason: $reason")

    if (reason.length > 10) {
        // approve temporarily
        allowTemporaryAccess()
    }

    lastReason = null
}

}


    // User genuinely left Instagram
    if (isBlockingActive && packageName != BLOCKED_PACKAGE) {
        isBlockingActive = false
        removeOverlay()
    }
Log.d(
    "TimeboxCoachFocus",
    "eventType=${event.eventType}, package=${event.packageName}, class=${event.className}"
)


}

private fun handleOverrideRequest(reason: String) {
    Log.d("TimeboxCoach", "Override requested with reason: $reason")

    // Firm baseline rules (MVP)
    if (reason.length < 10) {
        Log.d("TimeboxCoach", "Override denied: reason too short")
        return
    }

    if (containsImpulseLanguage(reason)) {
        Log.d("TimeboxCoach", "Override denied: impulsive language")
        return
    }

    // Approved: temporary unlock
    Log.d("TimeboxCoach", "Override approved for 5 minutes")
    allowTemporaryAccess()
}

private fun containsImpulseLanguage(text: String): Boolean {
    val lowered = text.lowercase()
    val impulseWords = listOf(
        "bored",
        "just checking",
        "timepass",
        "scroll",
        "nothing",
        "kill time"
    )
    return impulseWords.any { lowered.contains(it) }
}

private fun allowTemporaryAccess() {
    removeOverlay()
    isBlockingActive = false

    overlayView?.postDelayed({
        isBlockingActive = true
        showOverlay()
        Log.d("TimeboxCoach", "Temporary access expired")
    }, 5 * 60 * 1000) // 5 minutes
}


    private fun showOverlay() {
    if (overlayView != null) return

    val inflater =
        getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    overlayView = inflater.inflate(R.layout.block_overlay, null)

    // Button that launches reasoning Activity
    val explainButton =
        overlayView!!.findViewById<Button>(R.id.explainButton)

    explainButton.setOnClickListener {
    // 1. Remove overlay so Activity can appear
    removeOverlay()

    // 2. Launch reasoning Activity
    val intent = Intent(this, ReasonActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
        Log.d("TimeboxCoach", "Overlay added")
    } catch (e: Exception) {
        Log.e("TimeboxCoach", "Failed to add overlay", e)
    }
}


    private fun removeOverlay() {
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
                overlayView = null
            } catch (e: Exception) {
                Log.e("TimeboxCoach", "Failed to remove overlay", e)
            }
        }
    }

    override fun onInterrupt() {}
}

