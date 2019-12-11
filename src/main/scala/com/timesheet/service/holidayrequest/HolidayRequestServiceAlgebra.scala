package com.timesheet.service.holidayrequest

import java.time.LocalDate

import cats.data._
import com.timesheet.core.error.ValidationErrors.{BasicError, DateValidationError}
import com.timesheet.service.base.EntityServiceAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.holiday.HolidayType
import com.timesheet.model.holidayrequest.HolidayRequest
import com.timesheet.model.user.{User, UserId}

trait HolidayRequestServiceAlgebra[F[_]] extends EntityServiceAlgebra[F] {
  override type Entity = HolidayRequest

  def createHolidayRequest(
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
    holidayType: HolidayType,
    description: String,
  ): EitherT[F, DateValidationError, HolidayRequest]

  def deleteHolidayRequest(
    user: User,
    id: ID,
  ): EitherT[F, BasicError, HolidayRequest]

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
