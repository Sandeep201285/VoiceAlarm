package com.echocare.app.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

object EchoCareTelemetry {
    private const val TAG = "EchoCareTelemetry"
    
    data class TelemetryEvent(
        val timestamp: String,
        val eventName: String,
        val details: Map<String, Any>
    )

    private val eventBuffer = CopyOnWriteArrayList<TelemetryEvent>()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun logEvent(name: String, params: Map<String, Any> = emptyMap()) {
        val timestamp = dateFormatter.format(Date())
        val event = TelemetryEvent(timestamp, name, params)
        
        // Keep buffer size limited to 50
        if (eventBuffer.size >= 50) {
            eventBuffer.removeAt(0)
        }
        eventBuffer.add(event)
        
        Log.i(TAG, "EVENT: [$timestamp] $name -> $params")
    }

    fun logException(exception: Throwable, fatal: Boolean = false) {
        val timestamp = dateFormatter.format(Date())
        val name = if (fatal) "FATAL_CRASH_SIMULATED" else "NON_FATAL_EXCEPTION"
        val params = mapOf(
            "exception_class" to (exception.javaClass.name ?: "Unknown"),
            "message" to (exception.message ?: "No message"),
            "stacktrace" to exception.stackTrace.take(5).joinToString("\n") { it.toString() }
        )
        
        val event = TelemetryEvent(timestamp, name, params)
        if (eventBuffer.size >= 50) {
            eventBuffer.removeAt(0)
        }
        eventBuffer.add(event)
        
        if (fatal) {
            Log.wtf(TAG, "CRITICAL CRASH TRACE RECEIVED:", exception)
        } else {
            Log.e(TAG, "HANDLED EXCEPTION RECORDED:", exception)
        }
    }

    fun getLogs(): List<TelemetryEvent> {
        return eventBuffer.toList().reversed()
    }

    fun clearLogs() {
        eventBuffer.clear()
        logEvent("diagnostics_logs_cleared")
    }
}
