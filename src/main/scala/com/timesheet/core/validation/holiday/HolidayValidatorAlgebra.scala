package com.timesheet.core.validation.holiday

import java.time.LocalDate

import cats.data._
import com.timesheet.core.validation.ValidationUtils.HolidayValidationError
import com.timesheet.model.holiday.{Holiday, HolidayType}
import com.timesheet.model.user.User

trait HolidayValidatorAlgebra[F[_]] {
  def checkIfUserCanTakeHoliday(
    user: User,
    date: LocalDate,
    holidayType: HolidayType,
  ): EitherT[F, HolidayValidationError, Holiday]
}
