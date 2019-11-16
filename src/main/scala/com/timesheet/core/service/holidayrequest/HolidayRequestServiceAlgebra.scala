package com.timesheet.core.service.holidayrequest

import java.time.LocalDate

import cats.data._
import com.timesheet.core.validation.ValidationUtils.DateValidationError
import com.timesheet.model.holiday.HolidayType
import com.timesheet.model.holidayrequest.HolidayRequest
import com.timesheet.model.user.{User, UserId}

trait HolidayRequestServiceAlgebra[F[_]] {
  def createHolidayRequest(
    user: User,
    date: LocalDate,
    holidayType: HolidayType,
    description: String,
  ): EitherT[F, DateValidationError, HolidayRequest]

  def createHolidayRequestForDateRange(
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
    holidayType: HolidayType,
    description: String,
  ): EitherT[F, DateValidationError, List[HolidayRequest]]

  def deleteHolidayRequest(
    user: User,
    date: LocalDate,
  ): EitherT[F, DateValidationError, HolidayRequest]

  def deleteHolidayRequestsForDateRange(
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): EitherT[F, DateValidationError, List[HolidayRequest]]

  def getAllHolidayRequests(): F[List[HolidayRequest]]

  def getAllHolidayRequestsForSpecifiedDateRange(
    fromDate: LocalDate,
    toDate: LocalDate,
  ): EitherT[F, DateValidationError, List[HolidayRequest]]

  def getAllHolidayRequestsForUser(userId: UserId): F[List[HolidayRequest]]

  def getAllPendingHolidayRequestsForUser(userId: UserId): F[List[HolidayRequest]]

  def getAllHolidayRequestsForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): EitherT[F, DateValidationError, List[HolidayRequest]]

  def getAllPendingHolidayRequestsForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): EitherT[F, DateValidationError, List[HolidayRequest]]
}
