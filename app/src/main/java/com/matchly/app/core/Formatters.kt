package com.matchly.app.core

import com.matchly.app.data.model.DisplayScore
import com.matchly.app.data.model.MatchStatus
import com.matchly.app.data.model.StatusInfo
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Localised formatting of numbers, dates and times.
 * Digits are formatted in Latin first and then mapped (deterministically) to Arabic-Indic digits
 * when [arabicDigits] is on, so behaviour does not depend on device/ICU number-system defaults.
 */
class Formatters(
    val language: String,
    val arabicDigits: Boolean,
    val zone: ZoneId,
) {
    val locale: Locale = Locale.forLanguageTag(language)
    val isArabic: Boolean get() = language == "ar"

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    private val weekdayFmt = DateTimeFormatter.ofPattern("EEE", locale)
    private val dayMonthFmt = DateTimeFormatter.ofPattern("d MMM", locale)
    private val fullDateFmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", locale)
    private val dateTimeFmt = DateTimeFormatter.ofPattern("d MMM, HH:mm", locale)

    fun digits(text: String): String {
        if (!(arabicDigits && isArabic)) return text
        val sb = StringBuilder(text.length)
        for (ch in text) {
            if (ch in '0'..'9') sb.append('٠' + (ch - '0')) else sb.append(ch)
        }
        return sb.toString()
    }

    fun number(n: Int?): String = if (n == null) "-" else digits(n.toString())

    fun time(instant: Instant): String = digits(timeFmt.format(instant.atZone(zone)))

    fun dateTime(instant: Instant): String = digits(dateTimeFmt.format(instant.atZone(zone)))

    fun localDate(instant: Instant): LocalDate = instant.atZone(zone).toLocalDate()

    fun weekday(date: LocalDate): String = weekdayFmt.format(date)

    fun dayOfMonth(date: LocalDate): String = digits(date.dayOfMonth.toString())

    fun dayMonth(date: LocalDate): String = digits(dayMonthFmt.format(date))

    fun fullDate(date: LocalDate): String = digits(fullDateFmt.format(date))

    /** Match clock: "34'" or "45+2'". Null when the status has no running clock. */
    fun minute(status: StatusInfo): String? {
        val st = MatchStatus.fromCode(status.code)
        val clockRuns = st == MatchStatus.FIRST_HALF || st == MatchStatus.SECOND_HALF ||
            st == MatchStatus.EXTRA_TIME || st == MatchStatus.LIVE
        val elapsed = status.elapsed
        if (!clockRuns || elapsed == null) return null
        val extra = status.extra
        val text = if (extra != null && extra > 0) "$elapsed+$extra'" else "$elapsed'"
        return digits(text)
    }

    fun eventMinute(minute: Int?, extra: Int?): String {
        if (minute == null) return ""
        val text = if (extra != null && extra > 0) "$minute+$extra'" else "$minute'"
        return digits(text)
    }

    fun scoreLine(score: DisplayScore): String {
        if (!score.hasScore) return "-"
        return digits("${score.home} - ${score.away}")
    }

    fun penaltyLine(score: DisplayScore): String? {
        if (!score.hasPenalties) return null
        return digits("(${score.penaltyHome} - ${score.penaltyAway})")
    }

    companion object {
        fun from(settings: AppSettings): Formatters =
            Formatters(settings.effectiveLanguage, settings.arabicDigits, settings.zone)
    }
}
