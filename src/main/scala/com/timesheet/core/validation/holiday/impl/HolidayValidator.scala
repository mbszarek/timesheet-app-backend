package com.timesheet.core.validation.holiday.impl

import java.time.LocalDate

import cats._
import cats.implicits._
import cats.data._
import cats.effect._
import com.timesheet.core.validation.ValidationUtils
import com.timesheet.core.validation.ValidationUtils.HolidayValidationError
import com.timesheet.core.validation.holiday.HolidayValidatorAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.holiday.{Holiday, HolidayType}
import com.timesheet.model.user.User

final class HolidayValidator[F[_]: Applicative] extends HolidayValidatorAlgebra[F] {
  def checkIfUserCanTakeHoliday(
    user: User,
    date: LocalDate,
    holidayType: HolidayType,
  ): EitherT[F, ValidationUtils.HolidayValidationError, Holiday] =
    EitherT.fromEither[F] {
      Holiday(
        ID.createNew(),
        user.id,
        date,
        holidayType,
      ).asRight[HolidayValidationError]
    }
}

object HolidayValidator {
  def apply[F[_]: Applicative]: HolidayValidator[F] = new HolidayValidator[F]
}
