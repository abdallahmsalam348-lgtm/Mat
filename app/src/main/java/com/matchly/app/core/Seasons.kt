package com.matchly.app.core

import java.time.LocalDate
import java.time.ZoneId

object Seasons {
    /** European-style season start year: from July onwards the new season has started. */
    fun current(date: LocalDate): Int = if (date.monthValue >= 7) date.year else date.year - 1

    fun current(zone: ZoneId): Int = current(LocalDate.now(zone))
}
