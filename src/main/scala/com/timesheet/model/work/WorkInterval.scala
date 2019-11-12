package com.timesheet.model.work

import java.time.{LocalDate, LocalDateTime}

sealed trait WorkInterval extends Product with Serializable

object WorkInterval {
  final case class FiniteWorkInterval(
    from: LocalDateTime,
    to: LocalDateTime,
    wasAtWork: Boolean,
  ) extends WorkInterval

  final case class Holiday(date: LocalDate) extends WorkInterval
}
