package com.example

import com.example.data.model.AlertMode
import com.example.ui.FocusTimerState
import com.example.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `formatMinutes renders 3_45 PM accurately`() {
        val minutes = 15 * 60 + 45
        assertEquals("3:45 PM", DateTimeUtils.formatMinutes(minutes))
    }

    @Test
    fun `formatDurationRange renders start and end times`() {
        val start = 15 * 60 + 45
        val end = 16 * 60 + 30
        assertEquals("3:45 PM – 4:30 PM", DateTimeUtils.formatDurationRange(start, end))
    }

    @Test
    fun `nextOccurrenceMillis schedules recurring daily item in future`() {
        val now = System.currentTimeMillis()
        val next = DateTimeUtils.nextOccurrenceMillis(
            dateEpochDay = null,
            minutesOfDay = 15 * 60 + 45,
            repeatMask = DateTimeUtils.MASK_EVERY_DAY,
            nowMillis = now
        )
        assertNotNull(next)
        assertTrue((next ?: 0L) > now)
    }

    @Test
    fun `focus timer formats remaining mm_ss cleanly`() {
        val state = FocusTimerState(
            taskTitle = "Deep Work",
            totalMinutes = 25,
            remainingSeconds = 125,
            isRunning = true
        )
        assertTrue(state.isActive)
        assertEquals("02:05", state.formattedRemaining)
    }

    @Test
    fun `all four alert modes are available`() {
        assertEquals(4, AlertMode.entries.size)
    }
}
