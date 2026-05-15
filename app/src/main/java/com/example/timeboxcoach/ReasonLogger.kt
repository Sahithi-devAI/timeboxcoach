package com.example.timeboxcoach

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ReasonLogger {

    private const val FILE_NAME = "reason_logs.json"

    fun log(context: Context, log: ReasonLog) {
        val file = File(context.filesDir, FILE_NAME)

        val logsArray = if (file.exists()) {
            JSONArray(file.readText())
        } else {
            JSONArray()
        }

        val logObject = JSONObject().apply {
            put("timestamp", log.timestamp)
            put("blocked_app", log.blockedApp)
            put("allowed_window", log.allowedWindow)
            put("actual_time", log.actualTime)
            put("reason", log.reason)
            put("decision", log.decision)
        }

        logsArray.put(logObject)
        file.writeText(logsArray.toString(2))
    }
}
