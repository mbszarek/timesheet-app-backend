package com.timesheet.core.service

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}

package object work {
  implicit def instantOps(instant: Instant): InstantOps = new InstantOps(instant)

  implicit def localDateOps(localDate: LocalDate): LocalDateOps = new LocalDateOps(localDate)

  final class InstantOps(private val instant: Instant) extends AnyVal {
    def toLocalDate(): LocalDate =
      instant
        .atZone(ZoneId.systemDefault())
        .toLocalDate

    def toLocalDateTime(): LocalDateTime =
      instant
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime
  }

  final class LocalDateOps(private val localDate: LocalDate) extends AnyVal {
    def toInstant(): Instant =
      localDate
        .atStartOfDay()
        .atZone(ZoneId.systemDefault())
        .toInstant

    def toLocalDateTime(): LocalDateTime =
      localDate
        .atStartOfDay()
  }

}
