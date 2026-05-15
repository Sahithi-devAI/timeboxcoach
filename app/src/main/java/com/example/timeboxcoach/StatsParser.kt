package com.example.timeboxcoach

import android.content.Context
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatsParser {

    data class Entry(
        val timestamp: Long,
        val appName: String,
        val actualTime: String,
        val reason: String
    )

    data class Stats(
        val thisWeekTotal: Int,
        val lastWeekTotal: Int,
        val byApp: List<Pair<String, Int>>,
        val morning: Int,
        val afternoon: Int,
        val evening: Int,
        val night: Int,
        val recentEntries: List<Entry>
    )

    private val timeFmt = SimpleDateFormat("EEE HH:mm", Locale.ENGLISH)

    fun parse(context: Context): Stats {
        val file = File(context.filesDir, "reason_logs.json")
        if (!file.exists()) return empty()

        val nameMap = BlockRulesManager.getRules(context)
            .associate { it.packageName to it.appName }

        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000
        val fourteenDaysAgo = now - 14L * 24 * 60 * 60 * 1000

        val all = JSONArray(file.readText())
        val thisWeek = mutableListOf<Entry>()
        var lastWeekCount = 0

        for (i in 0 until all.length()) {
            val obj = all.getJSONObject(i)
            val ts = obj.getLong("timestamp")
            val pkg = obj.getString("blocked_app")
            when {
                ts >= sevenDaysAgo -> thisWeek.add(
                    Entry(
                        timestamp = ts,
                        appName = nameMap[pkg] ?: pkg,
                        actualTime = obj.getString("actual_time"),
                        reason = obj.getString("reason")
                    )
                )
                ts >= fourteenDaysAgo -> lastWeekCount++
            }
        }

        val hour = { t: String -> t.split(":").firstOrNull()?.toIntOrNull() ?: 0 }
        val morning = thisWeek.count { hour(it.actualTime) in 6..11 }
        val afternoon = thisWeek.count { hour(it.actualTime) in 12..16 }
        val evening = thisWeek.count { hour(it.actualTime) in 17..20 }
        val night = thisWeek.size - morning - afternoon - evening

        return Stats(
            thisWeekTotal = thisWeek.size,
            lastWeekTotal = lastWeekCount,
            byApp = thisWeek.groupBy { it.appName }
                .mapValues { it.value.size }
                .entries.sortedByDescending { it.value }
                .map { it.key to it.value },
            morning = morning,
            afternoon = afternoon,
            evening = evening,
            night = night,
            recentEntries = thisWeek.sortedByDescending { it.timestamp }
        )
    }

    fun formatTimestamp(ts: Long): String = timeFmt.format(Date(ts))

    private fun empty() = Stats(0, 0, emptyList(), 0, 0, 0, 0, emptyList())
}
