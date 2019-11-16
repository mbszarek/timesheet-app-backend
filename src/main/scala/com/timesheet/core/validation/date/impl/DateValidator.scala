package com.timesheet.core.validation
package date.impl

import java.time.LocalDateTime

import cats._
import cats.data._
import cats.implicits._
import com.timesheet.core.validation.ValidationUtils.{DateInTheFuture, DateInThePast, DateValidationError, WrongDateOrder}
import com.timesheet.core.validation.date.DateValidatorAlgebra
import com.timesheet.util.LocalDateTimeTypeClassInstances.localDateTimeOrderInstance

final class DateValidator[F[_]: Applicative] extends DateValidatorAlgebra[F] {
  def isDateInTheFuture(date: LocalDateTime): EitherT[F, ValidationUtils.DateValidationError, Unit] =
    EitherT.condUnit[F, DateValidationError](
      date >= LocalDateTime.now(),
      DateInThePast,
    )

  override def isDateInThePast(date: LocalDateTime): EitherT[F, DateValidationError, Unit] =
    EitherT.condUnit[F, DateValidationError](
      date <= LocalDateTime.now(),
      DateInTheFuture,
    )

  override def areDatesInProperOrder(
    firstDate: LocalDateTime,
    nextDate: LocalDateTime,
  ): EitherT[F, DateValidationError, Unit] =
    EitherT.condUnit[F, DateValidationError](
      firstDate <= nextDate,
      WrongDateOrder,
    )
}

object DateValidator {
  def apply[F[_]: Applicative]: DateValidator[F] = new DateValidator[F]()
}
