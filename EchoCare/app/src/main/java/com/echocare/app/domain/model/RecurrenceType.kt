package com.echocare.app.domain.model

enum class RecurrenceType(val label: String) {
    ONCE("One-time"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM("Custom interval")
}
