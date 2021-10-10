package io.pleo.antaeus.core

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

// Init with current time
var currentTime: Long = Date().time
    set(value) {
        println("Current time set to ${formatDateTo(value)}")
        field = value
    }


fun formatDateTo(timeInMillis: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    // Creating date format
    val simple: DateFormat = SimpleDateFormat(pattern)
    val result = Date(timeInMillis)
    return simple.format(result)
}

/**
 * Set the calendar to the first day of the next month.
 * Unless it's the first of the month
 */
fun setCalendarToFirstDayOfTheMonth(date: Date): Calendar {
    val cal = Calendar.getInstance()
    cal.time = date
    var year = cal.get(Calendar.YEAR)
    var month = cal.get(Calendar.MONTH)
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val hour = cal.get(Calendar.HOUR_OF_DAY)

    // if the day the server started is the first of the month
    // Note: timezones with UTC-11 and UTC-12 will be executed with 1 hour and 2 hour delay respectively
    if (day == 1 && hour < 10) { // hour < 10 as the biggest UTC+14
        cal.set(year, month, day, 0,0, 0)
        return cal
    }

    if (month == 12) {
        month = 1
        year += 1
    } else month += 1

    // set last day of the current month, as the UTC+14 as the maximum
    // set hour to 10 as if the invoice is to charged to be charged at midnight
    cal.set(year, month, 1, 0, 0, 0)
    return cal
}

fun getExecutionTime(
    timeZone: TimeZone,
    nowDate: Date,
    calendar: Calendar
): Long {
    val offset = timeZone.getOffset(calendar.timeInMillis) // offset in millis
    val diffNowToFirst = calendar.timeInMillis - nowDate.time
    val execTime = diffNowToFirst - offset
    return if (execTime < 0) return 0 else execTime
}