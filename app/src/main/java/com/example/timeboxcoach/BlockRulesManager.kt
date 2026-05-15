package com.example.timeboxcoach

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BlockRulesManager {
    private const val PREFS_NAME = "timebox_prefs"
    private const val KEY_RULES = "block_rules"

    fun getRules(context: Context): List<BlockRule> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RULES, "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                BlockRule(
                    packageName = obj.getString("packageName"),
                    appName = obj.getString("appName"),
                    allowedStart = obj.getString("allowedStart"),
                    allowedEnd = obj.getString("allowedEnd")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addRule(context: Context, rule: BlockRule) {
        val rules = getRules(context).toMutableList()
        rules.removeAll { it.packageName == rule.packageName }
        rules.add(rule)
        saveRules(context, rules)
    }

    fun removeRule(context: Context, packageName: String) {
        val rules = getRules(context).toMutableList()
        rules.removeAll { it.packageName == packageName }
        saveRules(context, rules)
    }

    fun getLastApprovedAt(context: Context, packageName: String): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong("approved_$packageName", 0L)

    fun setLastApprovedAt(context: Context, packageName: String, timestamp: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong("approved_$packageName", timestamp)
            .apply()
    }

    private fun saveRules(context: Context, rules: List<BlockRule>) {
        val array = JSONArray()
        rules.forEach { rule ->
            array.put(JSONObject().apply {
                put("packageName", rule.packageName)
                put("appName", rule.appName)
                put("allowedStart", rule.allowedStart)
                put("allowedEnd", rule.allowedEnd)
            })
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RULES, array.toString())
            .apply()
    }
}
