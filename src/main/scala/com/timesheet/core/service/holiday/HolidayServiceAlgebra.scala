package com.timesheet.core.service.holiday

import java.time.LocalDate

import cats.data._
import com.timesheet.core.validation.ValidationUtils.{DateValidationError, HolidayValidationError}
import com.timesheet.model.holiday.{Holiday, HolidayType}
import com.timesheet.model.user.{User, UserId}

trait HolidayServiceAlgebra[F[_]] {
  def createHoliday(
    user: User,
    date: LocalDate,
    holidayType: HolidayType,
  ): EitherT[F, HolidayValidationError, Holiday]

  def createHolidayForDateRange(
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
    holidayType: HolidayType,
  ): F[List[Either[HolidayValidationError, Holiday]]]

  def deleteHoliday(
    user: User,
    date: LocalDate,
  ): EitherT[F, HolidayValidationError, Holiday]

  def collectHolidaysForUser(userId: UserId): F[List[Holiday]]

  def collectHolidaysForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): EitherT[F, DateValidationError, List[Holiday]]
}
