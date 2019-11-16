package com.timesheet.core.validation.date

import java.time.LocalDateTime

import cats.data._
import com.timesheet.core.validation.ValidationUtils.DateValidationError

trait DateValidatorAlgebra[F[_]] {
  def isDateInTheFuture(date: LocalDateTime): EitherT[F, DateValidationError, Unit]

  def isDateInThePast(date: LocalDateTime): EitherT[F, DateValidationError, Unit]

  def areDatesInProperOrder(
    firstDate: LocalDateTime,
    nextDate: LocalDateTime,
  ): EitherT[F, DateValidationError, Unit]
}
