package com.timesheet.core.service.holidayrequest

import java.time.LocalDate

import cats._
import cats.data._
import cats.implicits._
import com.timesheet.core.validation.ValidationUtils.{HolidayRequestValidationError, HolidayValidationError}
import com.timesheet.model.holiday.HolidayType
import com.timesheet.model.holidayrequest.HolidayRequest
import com.timesheet.model.user.{User, UserId}

trait HolidayRequestServiceAlgebra[F[_]] {
  def createHolidayRequest(
    user: User,
    date: LocalDate,
    holidayType: HolidayType,
    description: String,
  ): EitherT[F, HolidayRequestValidationError, HolidayRequest]

  def createHolidayRequestForDateRange(
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
    holidayType: HolidayType,
    description: String,
  ): EitherT[F, HolidayRequestValidationError, List[HolidayRequest]]

  def deleteHolidayRequest(
    user: User,
    date: LocalDate,
  ): EitherT[F, HolidayRequestValidationError, HolidayRequest]

  def getAllHolidayRequests(): F[List[HolidayRequest]]

  def getAllHolidayRequestsForSpecifiedDateRange(
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[List[HolidayRequest]]

  def collectHolidayRequestsForUser(userId: UserId): F[List[HolidayRequest]]

  def collectHolidayRequestsForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[List[HolidayRequest]]
}
