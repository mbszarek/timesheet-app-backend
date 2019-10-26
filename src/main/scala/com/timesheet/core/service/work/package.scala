package com.timesheet.core.service

import java.time.{Instant, LocalDate, ZoneId}

package object work {
  implicit def instantOps(instant: Instant): InstantOps = new InstantOps(instant)

  implicit def localDateOps(localDate: LocalDate): LocalDateOps = new LocalDateOps(localDate)

  final class InstantOps(private val instant: Instant) extends AnyVal {
    def toLocalDate(): LocalDate =
      instant
        .atZone(ZoneId.systemDefault())
        .toLocalDate
  }

  final class LocalDateOps(private val localDate: LocalDate) extends AnyVal {
    def toInstant(): Instant =
      localDate
        .atStartOfDay()
        .atZone(ZoneId.systemDefault())
        .toInstant
  }

}
