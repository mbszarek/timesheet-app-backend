package com.timesheet.util

import cats.effect._
import cats.implicits._
import LocalDateTypeClassInstances.localDateOrderInstance
import java.time.LocalDate
import fs2.Stream

final class DateRangeGenerator[F[_]: Sync] {
  def getDateRange(
    from: LocalDate,
    to: LocalDate,
  ): F[List[LocalDate]] =
    stream(from, to).compile.toList

  private def stream(
    from: LocalDate,
    to: LocalDate,
  ): Stream[F, LocalDate] =
    Stream
      .iterate(from)(_.plusDays(1L))
      .takeWhile(_ <= to)
      .covary[F]
}

object DateRangeGenerator {
  def apply[F[_]: Sync]: DateRangeGenerator[F] =
    new DateRangeGenerator[F]()
}
