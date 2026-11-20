package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    private val dayShortFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val clockTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    /**
     * Local calendar epoch day (YYYY*10000 + MM*100 + DD) encoded via integer day index
     * using UTC-normalized midnight of the local calendar date so DST transitions never shift days.
     */
    fun todayEpochDay(): Long {
        val cal = Calendar.getInstance()
        return calendarToEpochDay(cal)
    }

    fun calendarToEpochDay(cal: Calendar): Long {
        val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        utc.clear()
        utc.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        return utc.timeInMillis / 86_400_000L
    }

    fun epochDayToCalendar(epochDay: Long): Calendar {
        val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = epochDay * 86_400_000L
        val local = Calendar.getInstance()
        local.clear()
        local.set(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
        return local
    }

    fun currentMinutesOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    /**
     * Returns 0 = Monday .. 6 = Sunday for bitmask matching.
     */
    fun weekdayIndex(epochDay: Long): Int {
        val cal = epochDayToCalendar(epochDay)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    fun matchesRepeatMask(epochDay: Long, repeatMask: Int): Boolean {
        if (repeatMask == 0) return false
        val idx = weekdayIndex(epochDay)
        return (repeatMask and (1 shl idx)) != 0
    }

    fun formatMinutes(minutesOfDay: Int): String {
        val safe = ((minutesOfDay % 1440) + 1440) % 1440
        val hour24 = safe / 60
        val min = safe % 60
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.getDefault(), "%d:%02d %s", hour12, min, amPm)
    }

    fun formatDurationRange(startMinutes: Int, endMinutes: Int?): String {
        return if (endMinutes != null && endMinutes > startMinutes) {
            "${formatMinutes(startMinutes)} – ${formatMinutes(endMinutes)}"
        } else {
            formatMinutes(startMinutes)
        }
    }

    fun formatShortWeekday(epochDay: Long): String {
        return dayShortFormat.format(epochDayToCalendar(epochDay).time)
    }

    fun dayOfMonth(epochDay: Long): Int {
        return epochDayToCalendar(epochDay).get(Calendar.DAY_OF_MONTH)
    }

    fun formatFullDate(epochDay: Long): String {
        val today = todayEpochDay()
        return when (epochDay) {
            today -> "Today · ${fullDateFormat.format(epochDayToCalendar(epochDay).time)}"
            today + 1 -> "Tomorrow · ${fullDateFormat.format(epochDayToCalendar(epochDay).time)}"
            today - 1 -> "Yesterday · ${fullDateFormat.format(epochDayToCalendar(epochDay).time)}"
            else -> fullDateFormat.format(epochDayToCalendar(epochDay).time)
        }
    }

    fun formatMonthYear(epochDay: Long): String {
        return monthYearFormat.format(epochDayToCalendar(epochDay).time)
    }

    fun formatTimestamp(millis: Long): String {
        return clockTimeFormat.format(Date(millis))
    }

    fun toTriggerMillis(epochDay: Long, minutesOfDay: Int): Long {
        val cal = epochDayToCalendar(epochDay)
        val safe = ((minutesOfDay % 1440) + 1440) % 1440
        cal.set(Calendar.HOUR_OF_DAY, safe / 60)
        cal.set(Calendar.MINUTE, safe % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun nextOccurrenceMillis(
        dateEpochDay: Long?,
        minutesOfDay: Int,
        repeatMask: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (repeatMask == 0) {
            val targetDay = dateEpochDay ?: return null
            val trigger = toTriggerMillis(targetDay, minutesOfDay)
            return if (trigger > nowMillis - 30_000L) trigger else null
        }

        val today = todayEpochDay()
        for (offset in 0..7) {
            val candidateDay = today + offset
            if (matchesRepeatMask(candidateDay, repeatMask)) {
                val candidateMillis = toTriggerMillis(candidateDay, minutesOfDay)
                if (candidateMillis > nowMillis + 5_000L) {
                    return candidateMillis
                }
            }
        }
        return null
    }

    fun formatRepeatLabel(repeatMask: Int): String {
        return when (repeatMask) {
            0 -> "Once"
            0b1111111 -> "Every day"
            0b0011111 -> "Weekdays"
            0b1100000 -> "Weekends"
            else -> {
                val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                labels.filterIndexed { index, _ -> (repeatMask and (1 shl index)) != 0 }
                    .joinToString(", ")
            }
        }
    }

    const val MASK_EVERY_DAY = 0b1111111
    const val MASK_WEEKDAYS = 0b0011111
    const val MASK_WEEKENDS = 0b1100000
}
