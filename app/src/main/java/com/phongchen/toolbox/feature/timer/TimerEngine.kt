package com.phongchen.toolbox.feature.timer

object TimerEngine {

    /** "MM:SS.CS" below an hour, "H:MM:SS.CS" at or above it. */
    fun formatElapsed(ms: Long): String {
        val centis = (ms / 10) % 100
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 60_000) % 60
        val hours = ms / 3_600_000
        return if (hours > 0) {
            "%d:%02d:%02d.%02d".format(hours, minutes, seconds, centis)
        } else {
            "%02d:%02d.%02d".format(minutes, seconds, centis)
        }
    }

    fun formatCountdown(ms: Long): String {
        val safe = ms.coerceAtLeast(0)
        val seconds = (safe / 1000) % 60
        val minutes = (safe / 60_000) % 60
        val hours = safe / 3_600_000
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}
