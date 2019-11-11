package com.timesheet.core.validation.date.impl

import java.time.LocalDate

import cats._
import cats.implicits._
import cats.data._
import com.timesheet.core.validation.ValidationUtils
import com.timesheet.core.validation.ValidationUtils.DateInTheFuture
import com.timesheet.core.validation.date.DateValidatorAlgebra
import com.timesheet.util.LocalDateTypeClassInstances.localDateOrderInstance

final class DateValidator[F[_]: Applicative] extends DateValidatorAlgebra[F] {
  def isDateInTheFuture(date: LocalDate): EitherT[F, ValidationUtils.DateValidationError, Unit] =
    EitherT.cond[F](
      date <= LocalDate.now(),
      (),
      DateInTheFuture,
    )
}

object DateValidator {
  def apply[F[_]: Applicative]: DateValidator[F] = new DateValidator[F]()
}
