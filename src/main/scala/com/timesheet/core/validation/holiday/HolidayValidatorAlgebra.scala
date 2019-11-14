package com.timesheet.core.validation.holiday

import java.time.LocalDate

import cats.data._
import com.timesheet.core.validation.ValidationUtils.{HolidayRequestValidationError, HolidayValidationError}
import com.timesheet.model.holiday.{Holiday, HolidayType}
import com.timesheet.model.holidayrequest.HolidayRequest
import com.timesheet.model.user.User

trait HolidayValidatorAlgebra[F[_]] {
  def checkIfUserCanTakeHoliday(
    user: User,
    date: LocalDate,
    holidayType: HolidayType,
  ): EitherT[F, HolidayValidationError, Holiday]

  def checkIfUserCanRequestHoliday(
    user: User,
    date: LocalDate,
    amountOfDays: Int,
    holidayType: HolidayType,
    description: String,
  ): EitherT[F, HolidayRequestValidationError, HolidayRequest]
}
