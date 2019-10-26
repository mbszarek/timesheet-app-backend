package com.timesheet.core.validation.date

import java.time.LocalDate

import cats.data._
import com.timesheet.core.validation.ValidationUtils.DateValidationError

trait DateValidatorAlgebra[F[_]] {
  def isDateInTheFuture(date: LocalDate): EitherT[F, DateValidationError, Unit]
}
