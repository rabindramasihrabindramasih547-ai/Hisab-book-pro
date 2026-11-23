package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToLong

object IndiaFormatters {
    val IST_TIMEZONE: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    private val EN_IN = Locale.Builder().setLanguage("en").setRegion("IN").build()

    fun istCalendar(timeMillis: Long = System.currentTimeMillis()): Calendar {
        return Calendar.getInstance(IST_TIMEZONE).apply {
            timeInMillis = timeMillis
        }
    }

    fun todayDateKey(): Long {
        val cal = istCalendar()
        return calendarToDateKey(cal)
    }

    fun calendarToDateKey(cal: Calendar): Long {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return y * 10000L + m * 100L + d
    }

    fun dateKeyToCalendar(dateKey: Long): Calendar {
        val y = (dateKey / 10000L).toInt().coerceIn(1970, 2100)
        val m = ((dateKey % 10000L) / 100L).toInt().coerceIn(1, 12) - 1
        val d = (dateKey % 100L).toInt().coerceIn(1, 31)
        return Calendar.getInstance(IST_TIMEZONE).apply {
            clear()
            set(y, m, d, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun formatDateKey(dateKey: Long): String {
        val y = (dateKey / 10000L).toInt()
        val m = ((dateKey % 10000L) / 100L).toInt()
        val d = (dateKey % 100L).toInt()
        return String.format(Locale.US, "%02d/%02d/%04d", d, m, y)
    }

    fun formatDateMillis(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
            timeZone = IST_TIMEZONE
        }
        return sdf.format(millis)
    }

    fun formatTimeMillis(millis: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", EN_IN).apply {
            timeZone = IST_TIMEZONE
        }
        return sdf.format(millis)
    }

    fun parseDdMmYyyyToDateKey(input: String): Long? {
        val trimmed = input.trim()
        val parts = trimmed.split("/", "-", ".")
        if (parts.size != 3) return null
        val d = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val y = parts[2].toIntOrNull() ?: return null
        if (y < 1970 || y > 2100 || m !in 1..12 || d !in 1..31) return null
        val cal = Calendar.getInstance(IST_TIMEZONE).apply {
            isLenient = false
            clear()
            set(y, m - 1, d)
        }
        return try {
            cal.timeInMillis
            y * 10000L + m * 100L + d
        } catch (_: Exception) {
            null
        }
    }

    fun addDays(dateKey: Long, days: Int): Long {
        val cal = dateKeyToCalendar(dateKey)
        cal.add(Calendar.DAY_OF_MONTH, days)
        return calendarToDateKey(cal)
    }

    fun startOfWeekDateKey(referenceKey: Long = todayDateKey()): Long {
        val cal = dateKeyToCalendar(referenceKey)
        cal.firstDayOfWeek = Calendar.MONDAY
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return calendarToDateKey(cal)
    }

    fun startOfMonthDateKey(referenceKey: Long = todayDateKey()): Long {
        return (referenceKey / 100L) * 100L + 1L
    }

    fun endOfMonthDateKey(referenceKey: Long = todayDateKey()): Long {
        val cal = dateKeyToCalendar(startOfMonthDateKey(referenceKey))
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (referenceKey / 100L) * 100L + lastDay
    }

    fun yearMonthKey(dateKey: Long): Long = dateKey / 100L

    fun monthRangeForOffset(monthsAgo: Int, referenceKey: Long = todayDateKey()): Pair<Long, Long> {
        val cal = dateKeyToCalendar(startOfMonthDateKey(referenceKey))
        cal.add(Calendar.MONTH, -monthsAgo)
        val start = calendarToDateKey(cal)
        val end = endOfMonthDateKey(start)
        return start to end
    }

    fun monthShortLabel(yearMonth: Long, isHindi: Boolean): String {
        val y = (yearMonth / 100L).toInt()
        val m = (yearMonth % 100L).toInt()
        val enMonths = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        val hiMonths = arrayOf(
            "जन", "फ़र", "मार्च", "अप्रै", "मई", "जून",
            "जुल", "अग", "सित", "अक्टू", "नव", "दिस"
        )
        val name = if (m in 1..12) {
            if (isHindi) hiMonths[m - 1] else enMonths[m - 1]
        } else "--"
        return "$name '${String.format(Locale.US, "%02d", y % 100)}"
    }

    /**
     * Formats paise (Long) using strict Indian numbering system:
     * e.g., 10000000L paise -> "₹1,00,000.00"
     * Negative values -> "-₹1,500.00"
     */
    fun formatInr(paise: Long, showDecimals: Boolean = true): String {
        val isNegative = paise < 0L
        val absPaise = if (paise == Long.MIN_VALUE) Long.MAX_VALUE else abs(paise)
        val rupees = absPaise / 100L
        val remPaise = absPaise % 100L

        val formattedRupees = formatIndianGrouping(rupees)
        val sign = if (isNegative) "-" else ""
        return if (showDecimals || remPaise != 0L) {
            String.format(Locale.US, "%s₹%s.%02d", sign, formattedRupees, remPaise)
        } else {
            "$sign₹$formattedRupees"
        }
    }

    fun formatInrCompact(paise: Long): String {
        val isNegative = paise < 0L
        val absRupees = abs(paise) / 100.0
        val sign = if (isNegative) "-" else ""
        return when {
            absRupees >= 1_00_00_000.0 -> String.format(Locale.US, "%s₹%.2fCr", sign, absRupees / 1_00_00_000.0)
            absRupees >= 1_00_000.0 -> String.format(Locale.US, "%s₹%.2fL", sign, absRupees / 1_00_000.0)
            else -> formatInr(paise, showDecimals = (abs(paise) % 100L != 0L))
        }
    }

    fun formatIndianGrouping(number: Long): String {
        val str = number.toString()
        if (str.length <= 3) return str
        val lastThree = str.takeLast(3)
        val remaining = str.dropLast(3)
        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count % 2 == 0 && i != 0) {
                sb.append(',')
            }
        }
        return sb.reverse().toString() + "," + lastThree
    }

    fun parseRupeesToPaise(input: String): Long? {
        val cleaned = input.trim().replace(",", "").replace("₹", "").trim()
        if (cleaned.isEmpty()) return null
        return try {
            val bd = BigDecimal(cleaned)
            bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    fun paiseToEditableString(paise: Long): String {
        if (paise == 0L) return ""
        val rupees = paise / 100L
        val rem = abs(paise % 100L)
        return if (rem == 0L) {
            rupees.toString()
        } else {
            String.format(Locale.US, "%d.%02d", rupees, rem)
        }
    }

    fun formatIndianMobile(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "+91 $digits"
            digits.length == 12 && digits.startsWith("91") -> "+91 ${digits.substring(2)}"
            raw.isBlank() -> ""
            else -> raw.trim()
        }
    }

    fun isValidIndianMobile(raw: String): Boolean {
        val digits = raw.filter { it.isDigit() }
        val tenDigits = when {
            digits.length == 10 -> digits
            digits.length == 12 && digits.startsWith("91") -> digits.substring(2)
            digits.length == 11 && digits.startsWith("0") -> digits.substring(1)
            else -> return false
        }
        return tenDigits.firstOrNull() in listOf('6', '7', '8', '9')
    }

    fun calculateHoursBetween(startHHmm: String, endHHmm: String): Double? {
        val sParts = startHHmm.trim().split(":")
        val eParts = endHHmm.trim().split(":")
        if (sParts.size != 2 || eParts.size != 2) return null
        val sh = sParts[0].toIntOrNull() ?: return null
        val sm = sParts[1].toIntOrNull() ?: return null
        val eh = eParts[0].toIntOrNull() ?: return null
        val em = eParts[1].toIntOrNull() ?: return null
        if (sh !in 0..23 || eh !in 0..23 || sm !in 0..59 || em !in 0..59) return null
        var diffMinutes = (eh * 60 + em) - (sh * 60 + sm)
        if (diffMinutes < 0) diffMinutes += 24 * 60
        return ((diffMinutes / 60.0) * 100.0).roundToLong() / 100.0
    }
}
