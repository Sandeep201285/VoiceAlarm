package com.echocare.app.util

import com.echocare.app.domain.model.Reminder
import com.echocare.app.domain.model.RecurrenceType

object BackupHelper {
    
    fun exportToBackup(reminders: List<Reminder>): String {
        val sb = StringBuilder()
        sb.append("[\n")
        reminders.forEachIndexed { index, r ->
            sb.append("  {\n")
            sb.append("    \"title\": \"${r.title.replace("\"", "\\\"")}\",\n")
            sb.append("    \"message\": \"${r.message.replace("\"", "\\\"")}\",\n")
            sb.append("    \"triggerTime\": ${r.triggerTime},\n")
            sb.append("    \"recurrenceType\": \"${r.recurrenceType.name}\",\n")
            sb.append("    \"recurrenceIntervalMs\": ${r.recurrenceIntervalMs},\n")
            sb.append("    \"repeatIntervalMinutes\": ${r.repeatIntervalMinutes},\n")
            sb.append("    \"maxRepetitions\": ${r.maxRepetitions},\n")
            sb.append("    \"isActive\": ${r.isActive},\n")
            sb.append("    \"creatorId\": \"${r.creatorId.replace("\"", "\\\"")}\",\n")
            sb.append("    \"recipientId\": ${if (r.recipientId != null) "\"${r.recipientId!!.replace("\"", "\\\"")}\"" else "null"},\n")
            sb.append("    \"syncStatus\": \"${r.syncStatus}\"\n")
            sb.append("  }")
            if (index < reminders.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("]")
        return sb.toString()
    }

    fun importFromBackup(json: String): List<Reminder> {
        val list = mutableListOf<Reminder>()
        try {
            val cleanJson = json.trim()
            if (!cleanJson.startsWith("[") || !cleanJson.endsWith("]")) return emptyList()
            
            // Extract the body inside [ and ]
            val inner = cleanJson.substring(1, cleanJson.length - 1).trim()
            if (inner.isEmpty()) return emptyList()

            // Split by object brackets: "}," or "}\n,"
            val objects = inner.split(Regex("(?<=}),\\s*"))
            for (obj in objects) {
                val cleanObj = obj.trim()
                if (cleanObj.isEmpty()) continue
                
                val title = extractJsonStringValue(cleanObj, "title") ?: ""
                val message = extractJsonStringValue(cleanObj, "message") ?: ""
                val triggerTime = extractJsonLongValue(cleanObj, "triggerTime") ?: System.currentTimeMillis()
                val recTypeName = extractJsonStringValue(cleanObj, "recurrenceType") ?: "ONCE"
                val recurrenceType = try { RecurrenceType.valueOf(recTypeName) } catch (e: Exception) { RecurrenceType.ONCE }
                val recurrenceIntervalMs = extractJsonLongValue(cleanObj, "recurrenceIntervalMs") ?: 0L
                val repeatIntervalMinutes = extractJsonIntValue(cleanObj, "repeatIntervalMinutes") ?: 5
                val maxRepetitions = extractJsonIntValue(cleanObj, "maxRepetitions") ?: 3
                val isActive = extractJsonBooleanValue(cleanObj, "isActive") ?: true
                val creatorId = extractJsonStringValue(cleanObj, "creatorId") ?: "local_user"
                val recipientId = extractJsonStringValue(cleanObj, "recipientId")
                val syncStatus = extractJsonStringValue(cleanObj, "syncStatus") ?: "SYNCED"

                list.add(
                    Reminder(
                        title = title,
                        message = message,
                        triggerTime = triggerTime,
                        recurrenceType = recurrenceType,
                        recurrenceIntervalMs = recurrenceIntervalMs,
                        repeatIntervalMinutes = repeatIntervalMinutes,
                        maxRepetitions = maxRepetitions,
                        isActive = isActive,
                        creatorId = creatorId,
                        recipientId = recipientId,
                        syncStatus = syncStatus
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun extractJsonStringValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*(?:\"([^\"]*)\"|(null))".toRegex()
        val match = pattern.find(json) ?: return null
        val groupVal = match.groups[1]?.value
        if (groupVal == null || groupVal == "null") return null
        return groupVal.replace("\\\"", "\"")
    }

    private fun extractJsonLongValue(json: String, key: String): Long? {
        val pattern = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
        val match = pattern.find(json) ?: return null
        return match.groups[1]?.value?.toLongOrNull()
    }

    private fun extractJsonIntValue(json: String, key: String): Int? {
        val pattern = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
        val match = pattern.find(json) ?: return null
        return match.groups[1]?.value?.toIntOrNull()
    }

    private fun extractJsonBooleanValue(json: String, key: String): Boolean? {
        val pattern = "\"$key\"\\s*:\\s*(true|false)".toRegex()
        val match = pattern.find(json) ?: return null
        return match.groups[1]?.value?.toBoolean()
    }
}
