package com.echocare.app.util

import com.echocare.app.domain.model.RecurrenceType
import java.util.*
import java.util.regex.Pattern

data class NlpResult(
    val title: String,
    val message: String,
    val triggerTime: Long,
    val recurrenceType: RecurrenceType,
    val recurrenceIntervalMs: Long = 0L,
    val recipient: String? = null
)

object NlpParser {

    /**
     * Parse the natural language transcript to extract reminder details.
     */
    fun parse(text: String, currentTimeMs: Long = System.currentTimeMillis()): NlpResult {
        val cleanText = text.trim().replace(Pattern.compile("\\s+").toRegex(), " ")
        var msg = cleanText
        var recipient: String? = null
        var recurrenceType = RecurrenceType.ONCE
        var recurrenceIntervalMs = 0L

        // 1. Recipient extraction
        // English: "tell [Name] to [Message]", "remind [Name] to [Message]"
        val enTellPattern = Pattern.compile("^(?:tell|remind)\\s+([a-zA-Z\\u0900-\\u097F]+)\\s+to\\s+(.*)$", Pattern.CASE_INSENSITIVE)
        val enTellMatcher = enTellPattern.matcher(cleanText)
        if (enTellMatcher.find()) {
            val name = enTellMatcher.group(1)
            if (name.lowercase(Locale.ENGLISH) != "me" && name.lowercase(Locale.ENGLISH) != "myself") {
                recipient = name
                msg = enTellMatcher.group(2)
            } else {
                msg = enTellMatcher.group(2)
            }
        }

        // Hindi: "[Name] को [Message] के लिए कहो/याद दिलाओ" or "[Name] को [Message] कहो"
        // E.g., "पापा को दवाई लेने के लिए कहो" -> Papa ko ... kaho
        val hiTellPattern1 = Pattern.compile("^([\\u0900-\\u097F]+)\\s+को\\s+(.*)\\s+(?:के\\s+लिए\\s+)?(?:कहो|याद\\s+दिलाओ)$", Pattern.CASE_INSENSITIVE)
        val hiTellMatcher1 = hiTellPattern1.matcher(msg)
        if (hiTellMatcher1.find()) {
            val name = hiTellMatcher1.group(1)
            if (name != "मुझे" && name != "मुझको") {
                recipient = name
                msg = hiTellMatcher1.group(2)
            } else {
                msg = hiTellMatcher1.group(2)
            }
        }

        // 2. Recurrence Extraction
        val lowerMsg = msg.lowercase(Locale.getDefault())

        // CUSTOM Hourly
        // "every hour", "every 2 hours", "every two hours", "हर घंटे", "हर 2 घंटे में"
        val hrInterval = parseCustomHourly(lowerMsg)
        if (hrInterval > 0L) {
            recurrenceType = RecurrenceType.CUSTOM
            recurrenceIntervalMs = hrInterval
        } else if (lowerMsg.contains("every day") || lowerMsg.contains("daily") || 
                   lowerMsg.contains("रोज़") || lowerMsg.contains("रोजाना") || lowerMsg.contains("हर दिन") || lowerMsg.contains("प्रतिदिन")) {
            recurrenceType = RecurrenceType.DAILY
        } else if (lowerMsg.contains("every week") || lowerMsg.contains("weekly") || 
                   lowerMsg.contains("हर हफ्ते") || lowerMsg.contains("हर सप्ताह") || lowerMsg.contains("साप्ताहिक")) {
            recurrenceType = RecurrenceType.WEEKLY
        } else if (lowerMsg.contains("every month") || lowerMsg.contains("monthly") || 
                   lowerMsg.contains("हर महीने") || lowerMsg.contains("हर माह") || lowerMsg.contains("मासिक")) {
            recurrenceType = RecurrenceType.MONTHLY
        } else {
            // Check for specific day of the week, e.g. "every Friday"
            val dayOfWeek = parseDayOfWeek(lowerMsg)
            if (dayOfWeek != -1) {
                recurrenceType = RecurrenceType.WEEKLY
            }
        }

        // 3. Time & Date Extraction
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimeMs
        
        var timeSet = false
        var dateSet = false

        // Minutes/hours from now relative triggers
        // E.g. "in 30 minutes", "in 2 hours", "30 मिनट बाद"
        val minFromNowPattern = Pattern.compile("in\\s+(\\d+)\\s*(?:min|minute|minutes)|(\\d+)\\s*(?:min|minute|minutes)\\s+from\\s+now|(\\d+)\\s*मिनट\\s*बाद", Pattern.CASE_INSENSITIVE)
        val minFromNowMatcher = minFromNowPattern.matcher(lowerMsg)
        if (minFromNowMatcher.find()) {
            val minutesStr = minFromNowMatcher.group(1) ?: minFromNowMatcher.group(2) ?: minFromNowMatcher.group(3)
            val minutes = minutesStr.toIntOrNull() ?: 0
            calendar.add(Calendar.MINUTE, minutes)
            timeSet = true
            dateSet = true
        } else {
            val hrFromNowPattern = Pattern.compile("in\\s+(\\d+)\\s*(?:hr|hour|hours)|(\\d+)\\s*(?:hr|hour|hours)\\s+from\\s+now|(\\d+)\\s*घंटे\\s*बाद", Pattern.CASE_INSENSITIVE)
            val hrFromNowMatcher = hrFromNowPattern.matcher(lowerMsg)
            if (hrFromNowMatcher.find()) {
                val hoursStr = hrFromNowMatcher.group(1) ?: hrFromNowMatcher.group(2) ?: hrFromNowMatcher.group(3)
                val hours = hoursStr.toIntOrNull() ?: 0
                calendar.add(Calendar.HOUR_OF_DAY, hours)
                timeSet = true
                dateSet = true
            }
        }

        // Absolute Time triggers:
        // E.g. "at 5:30 AM", "at 8 PM", "5 बजे", "रात 8 बजे", "सुबह 5 बजे"
        if (!timeSet) {
            val parsedTime = parseAbsoluteTime(lowerMsg)
            if (parsedTime != null) {
                val (hour, minute) = parsedTime
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                timeSet = true
            }
        }

        // Absolute Date / Relative Date triggers
        // E.g. "tomorrow", "कल", "today", "आज"
        if (lowerMsg.contains("tomorrow") || lowerMsg.contains("कल")) {
            val today = Calendar.getInstance()
            today.timeInMillis = currentTimeMs
            calendar.set(Calendar.YEAR, today.get(Calendar.YEAR))
            calendar.set(Calendar.MONTH, today.get(Calendar.MONTH))
            calendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            dateSet = true
        } else if (lowerMsg.contains("today") || lowerMsg.contains("आज")) {
            val today = Calendar.getInstance()
            today.timeInMillis = currentTimeMs
            calendar.set(Calendar.YEAR, today.get(Calendar.YEAR))
            calendar.set(Calendar.MONTH, today.get(Calendar.MONTH))
            calendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH))
            dateSet = true
        } else {
            // Check if user specified a day of week like "every Friday" or "on Friday"
            val dayOfWeek = parseDayOfWeek(lowerMsg)
            if (dayOfWeek != -1) {
                val today = Calendar.getInstance()
                today.timeInMillis = currentTimeMs
                calendar.set(Calendar.YEAR, today.get(Calendar.YEAR))
                calendar.set(Calendar.MONTH, today.get(Calendar.MONTH))
                calendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH))
                
                var daysDiff = dayOfWeek - calendar.get(Calendar.DAY_OF_WEEK)
                if (daysDiff <= 0) daysDiff += 7
                calendar.add(Calendar.DAY_OF_MONTH, daysDiff)
                dateSet = true
            }
        }

        // If time was set but date was not specified, and the time is already in the past, default to tomorrow
        if (timeSet && !dateSet && calendar.timeInMillis <= currentTimeMs) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // If neither was set, default to 2 minutes from now
        if (!timeSet && !dateSet) {
            calendar.timeInMillis = currentTimeMs + 2 * 60 * 1000L
        }

        // 4. Message Cleanup
        // Remove NLP tags and triggers from the final text
        var finalMsg = msg
        val stopPhrases = listOf(
            "at \\d+(?::\\d+)?\\s*(?:am|pm)?",
            "at \\d+\\s*(?:am|pm)",
            "\\d+\\s*बजे",
            "tomorrow", "कल", "today", "आज",
            "every day", "daily", "रोज़", "रोजाना", "हर दिन", "प्रतिदिन",
            "every week", "weekly", "हर हफ्ते", "साप्ताहिक",
            "every month", "monthly", "हर महीने", "मासिक",
            "every Friday", "every Monday", "every Tuesday", "every Wednesday", "every Thursday", "every Saturday", "every Sunday",
            "on Friday", "on Monday", "on Tuesday", "on Wednesday", "on Thursday", "on Saturday", "on Sunday",
            "in \\d+\\s*(?:min|minute|minutes|hr|hour|hours)",
            "\\d+\\s*(?:min|minute|minutes|hr|hour|hours)\\s+from\\s+now",
            "\\d+\\s*(?:मिनट|घंटे)\\s*बाद",
            "every \\d+\\s*hours", "every hour", "हर \\d+\\s*घंटे\\s*में", "हर घंटे",
            "सुबह", "शाम", "रात", "दोपहर"
        )
        
        for (phrase in stopPhrases) {
            finalMsg = Pattern.compile(phrase, Pattern.CASE_INSENSITIVE).matcher(finalMsg).replaceAll("")
        }

        // Cleanup punctuation and multiple spaces
        finalMsg = finalMsg.replace(Pattern.compile("[,.?!-]").toRegex(), " ")
        finalMsg = finalMsg.replace(Pattern.compile("\\s+").toRegex(), " ").trim()

        // Default msg if empty
        if (finalMsg.isBlank()) {
            finalMsg = if (recipient != null) "Reminder for $recipient" else "Medicine reminder"
        }

        // Cap first letter
        val title = finalMsg.split(" ").take(3).joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

        return NlpResult(
            title = title,
            message = if (recipient != null) "$recipient, $finalMsg" else finalMsg,
            triggerTime = calendar.timeInMillis,
            recurrenceType = recurrenceType,
            recurrenceIntervalMs = recurrenceIntervalMs,
            recipient = recipient
        )
    }

    private fun parseCustomHourly(text: String): Long {
        // Match English: "every hour" -> 1 hr, "every 2 hours" -> 2 hr, "every two hours" -> 2 hr
        val enPattern = Pattern.compile("every\\s+(?:(\\d+)|one|two|three|four|five|six|seven|eight|nine|ten)?\\s*hours?", Pattern.CASE_INSENSITIVE)
        val enMatcher = enPattern.matcher(text)
        if (enMatcher.find()) {
            val numStr = enMatcher.group(1)
            val hours = if (numStr != null) {
                numStr.toLongOrNull() ?: 1L
            } else {
                val phrase = enMatcher.group(0).lowercase(Locale.ENGLISH)
                when {
                    phrase.contains("two") -> 2L
                    phrase.contains("three") -> 3L
                    phrase.contains("four") -> 4L
                    phrase.contains("five") -> 5L
                    phrase.contains("six") -> 6L
                    else -> 1L
                }
            }
            return hours * 3600_000L
        }

        // Match Hindi: "हर घंटे" -> 1 hr, "हर 2 घंटे में" -> 2 hr, "हर दो घंटे में" -> 2 hr
        val hiPattern = Pattern.compile("हर\\s+(?:(\\d+)|एक|दो|तीन|चार|पांच|छह)?\\s*घंटे", Pattern.CASE_INSENSITIVE)
        val hiMatcher = hiPattern.matcher(text)
        if (hiMatcher.find()) {
            val numStr = hiMatcher.group(1)
            val hours = if (numStr != null) {
                numStr.toLongOrNull() ?: 1L
            } else {
                val phrase = hiMatcher.group(0)
                when {
                    phrase.contains("दो") -> 2L
                    phrase.contains("तीन") -> 3L
                    phrase.contains("चार") -> 4L
                    phrase.contains("पांच") -> 5L
                    phrase.contains("छह") -> 6L
                    else -> 1L
                }
            }
            return hours * 3600_000L
        }

        return 0L
    }

    private fun parseDayOfWeek(text: String): Int {
        val days = mapOf(
            "sunday" to Calendar.SUNDAY, "monday" to Calendar.MONDAY, "tuesday" to Calendar.TUESDAY,
            "wednesday" to Calendar.WEDNESDAY, "thursday" to Calendar.THURSDAY, "friday" to Calendar.FRIDAY,
            "saturday" to Calendar.SATURDAY,
            "रविवार" to Calendar.SUNDAY, "सोमवार" to Calendar.MONDAY, "मंगलवार" to Calendar.TUESDAY,
            "बुधवार" to Calendar.WEDNESDAY, "गुरुवार" to Calendar.THURSDAY, "शुक्रवार" to Calendar.FRIDAY,
            "शनिवार" to Calendar.SATURDAY
        )
        for ((dayStr, dayVal) in days) {
            if (text.contains(dayStr)) return dayVal
        }
        return -1
    }

    private fun parseAbsoluteTime(text: String): Pair<Int, Int>? {
        // E.g. "at 5:30 AM", "at 8 PM", "5:30 बजे", "रात 8 बजे"
        val timePattern = Pattern.compile("(\\d+)(?::(\\d+))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
        val matcher = timePattern.matcher(text)
        
        // Let's loop through matches to find one that seems like a time trigger
        while (matcher.find()) {
            val hrStr = matcher.group(1)
            val minStr = matcher.group(2)
            val ampmStr = matcher.group(3)

            val rawHour = hrStr.toIntOrNull() ?: continue
            val minute = minStr?.toIntOrNull() ?: 0

            if (rawHour !in 0..23 || minute !in 0..59) continue

            var hour = rawHour

            // Check English AM/PM
            if (ampmStr != null) {
                if (ampmStr.lowercase(Locale.ENGLISH) == "pm" && hour < 12) hour += 12
                if (ampmStr.lowercase(Locale.ENGLISH) == "am" && hour == 12) hour = 0
            } else {
                // Check Hindi context (सुबह = morning, शाम/दोपहर = afternoon/evening, रात = night)
                val isPm = text.contains("शाम") || text.contains("रात") || text.contains("दोपहर") || text.contains("evening") || text.contains("night") || text.contains("afternoon")
                val isAm = text.contains("सुबह") || text.contains("भोर") || text.contains("morning")
                if (isPm && hour < 12) hour += 12
                if (isAm && hour == 12) hour = 0
            }

            return Pair(hour, minute)
        }

        // Hindi fallback: search "(\d+) बजे"
        val hiBajePattern = Pattern.compile("(\\d+)(?:\\s*:\\s*(\\d+))?\\s*बजे", Pattern.CASE_INSENSITIVE)
        val hiMatcher = hiBajePattern.matcher(text)
        if (hiMatcher.find()) {
            val hrStr = hiMatcher.group(1)
            val minStr = hiMatcher.group(2)
            val rawHour = hrStr.toIntOrNull() ?: 12
            val minute = minStr?.toIntOrNull() ?: 0
            var hour = rawHour
            val isPm = text.contains("शाम") || text.contains("रात") || text.contains("दोपहर")
            val isAm = text.contains("सुबह")
            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0
            return Pair(hour, minute)
        }

        return null
    }
}
