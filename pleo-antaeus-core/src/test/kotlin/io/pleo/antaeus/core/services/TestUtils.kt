package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.pleo.antaeus.core.getExecutionTime
import io.pleo.antaeus.core.setCalendarToFirstDayOfTheMonth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*


class TestUtils {

    private val hourInMillis = 3600000

    // ------ Testing getExecutionTime() ------
    @Test
    fun `execution time for 2 hours in future timezone UTC`() {
        val nowDate = mockk<Date>()
            every { nowDate.time } returns 0L

        val calendar2HoursFuture = mockk<Calendar>()
            every { calendar2HoursFuture.timeInMillis } returns hourInMillis * 2L

        val timezone = mockk<TimeZone>()
            every { timezone.getOffset(any()) } returns 0

        val execTime = getExecutionTime(timezone, nowDate, calendar2HoursFuture)
        assertEquals(hourInMillis * 2L, execTime)
    }

    @Test
    fun `execution time for 2 hours in future timezone UTC+2`() {
        val nowDate = mockk<Date> {
            every { time } returns 0L
        }
        val calendar2HoursFuture = mockk<Calendar> {
            every { timeInMillis } returns hourInMillis * 2L
        }
        val timezone = mockk<TimeZone> {
            every { getOffset(any()) } returns hourInMillis * 2
        }
        val execTime = getExecutionTime(timezone, nowDate, calendar2HoursFuture)
        assertEquals(0, execTime)
    }

    @Test
    fun `execution time for 5 hours in future timezone UTC-2`() {
        val nowDate = mockk<Date> {
            every { time } returns 0L
        }
        val calendar5HoursFuture = mockk<Calendar> {
            every { timeInMillis } returns hourInMillis * 5L
        }
        val timezone = mockk<TimeZone> {
            every { getOffset(any()) } returns -hourInMillis * 2
        }
        val execTime = getExecutionTime(timezone, nowDate, calendar5HoursFuture)
        assertEquals(hourInMillis * 7L, execTime)
    }

    @Test
    fun `execution time for now timezone UTC+14`() {
        val nowDate = mockk<Date> {
            every { time } returns 0L
        }
        val calendar5HoursFuture = mockk<Calendar> {
            every { timeInMillis } returns 0L
        }
        val timezone = mockk<TimeZone> {
            every { getOffset(any()) } returns hourInMillis * 14
        }
        val execTime = getExecutionTime(timezone, nowDate, calendar5HoursFuture)
        assertEquals(0L, execTime)
    }

    @Test
    fun `execution time with nowDate set to 9hours 59 min and 59 sec timezone UTC-12`() {
        val nowDate = mockk<Date> {
            every { time } returns 35999000L
        }
        val calendar0HoursFuture = mockk<Calendar> {
            every { timeInMillis } returns 0L
        }
        val timezone = mockk<TimeZone> {
            every { getOffset(any()) } returns -hourInMillis * 12
        }
        val execTime = getExecutionTime(timezone, nowDate, calendar0HoursFuture)
        val expectedExecTime: Long = hourInMillis * 2 + 1000L
        assertEquals(expectedExecTime, execTime)
    }

    @Test
    fun `execution time with nowDate set to 9hours 59 min and 59 sec timezone UTC-11`() {
        val nowDate = mockk<Date> {
            every { time } returns 35999000L
        }
        val calendar0HoursFuture = mockk<Calendar> {
            every { timeInMillis } returns 0L
        }
        val timezone = mockk<TimeZone> {
            every { getOffset(any()) } returns -hourInMillis * 11
        }
        val execTime = getExecutionTime(timezone, nowDate, calendar0HoursFuture)
        val expectedExecTime: Long = hourInMillis + 1000L
        assertEquals(expectedExecTime, execTime)
    }

    // ------ Testing setCalendarToFirstDayOfTheMonth() ------
    @Test
    fun `set calendar to the first day of the next month`() {
        val fifthOfJan1970Date = mockk<Date> {
             every{ time } returns 345600000
        }
        val firstOfFeb1970 = 2678400000L
        val firstOfTheMonth = setCalendarToFirstDayOfTheMonth(fifthOfJan1970Date)
        assertEquals(firstOfFeb1970, firstOfTheMonth.timeInMillis)
    }

    @Test
    fun `set calendar to the first day of the next month while being first of the month and 5 hours`() {
        val firstOfJan1970and5hourDate = mockk<Date> {
            every{ time } returns 18000000
        }
        val firstOfJan1970 = 0L
        val firstOfTheMonth = setCalendarToFirstDayOfTheMonth(firstOfJan1970and5hourDate)
        assertEquals(firstOfJan1970, firstOfTheMonth.timeInMillis)
    }

}