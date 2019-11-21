package com.timesheet.core.validation.holiday

import java.time.LocalDate

import cats.data._
import com.timesheet.core.validation.ValidationUtils.{HolidayRequestValidationError, HolidayValidationError}
import com.timesheet.model.holiday.HolidayType
import com.timesheet.model.user.User

trait HolidayValidatorAlgebra[F[_]] {
  def checkIfUserCanRequestHoliday(
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
    holidayType: HolidayType,
  ): EitherT[F, HolidayRequestValidationError, Unit]
}
