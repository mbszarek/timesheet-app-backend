package com.timesheet.core.service.holidayrequest.impl

import java.time.LocalDate

import cats.data.EitherT
import cats.effect.Concurrent
import cats.implicits._
import com.timesheet.core.service.holidayrequest.HolidayRequestServiceAlgebra
import com.timesheet.core.store.holidayrequest.HolidayRequestStoreAlgebra
import com.timesheet.core.validation.ValidationUtils.{DateValidationError, HolidayRequestNotFound}
import com.timesheet.core.validation.date.DateValidatorAlgebra
import com.timesheet.core.validation.holiday.HolidayValidatorAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.holiday.HolidayType
import com.timesheet.model.holidayrequest.{HolidayRequest, Status}
import com.timesheet.model.user.{User, UserId}
import com.timesheet.util.DateRangeGenerator
import fs2._

final class HolidayRequestService[F[_]: Concurrent](
  dateValidator: DateValidatorAlgebra[F],
  holidayValidator: HolidayValidatorAlgebra[F],
  holidayRequestStore: HolidayRequestStoreAlgebra[F],
) extends HolidayRequestServiceAlgebra[F] {

  override def createHolidayRequest(
    user: User,
    date: LocalDate,
    holidayType: HolidayType,
    description: String,
  ): EitherT[F, DateValidationError, HolidayRequest] =
    for {
      _ <- dateValidator
        .isDateInTheFuture(date.atStartOfDay())

      _ <- holidayValidator
        .checkIfUserCanRequestHoliday(user, date, 1, holidayType)

      result <- EitherT
        .right[DateValidationError] {
          holidayRequestStore
            .create(createHolidayRequestEntity(user.id, date, holidayType, description))
        }
    } yield result

  override def createHolidayRequestForDateRange(
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
    holidayType: HolidayType,
    description: String,
  ): EitherT[F, DateValidationError, List[HolidayRequest]] =
    for {
      _ <- dateValidator
        .isDateInTheFuture(fromDate.atStartOfDay())

      _ <- dateValidator
        .areDatesInProperOrder(fromDate.atStartOfDay(), toDate.atStartOfDay())

      dates <- EitherT
        .right[DateValidationError](DateRangeGenerator[F].getDateRange(fromDate, toDate))

      _ <- holidayValidator
        .checkIfUserCanRequestHoliday(user, dates.head, dates.size, holidayType)

      result <- EitherT
        .right[DateValidationError] {
          Stream
            .apply(dates: _*)
            .covary[F]
            .evalMap { date =>
              holidayRequestStore.create(createHolidayRequestEntity(user.id, date, holidayType, description))
            }
            .compile
            .toList
        }
    } yield result

  override def deleteHolidayRequest(
    user: User,
    date: LocalDate,
  ): EitherT[F, DateValidationError, HolidayRequest] =
    EitherT {
      holidayRequestStore
        .deleteUserHolidayRequestForDate(user.id, date)
        .fold[Either[DateValidationError, HolidayRequest]](
          HolidayRequestNotFound(
            user,
            date,
          ).asLeft[HolidayRequest],
        )(_.asRight[DateValidationError])
    }

  override def deleteHolidayRequestsForDateRange(
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): EitherT[F, DateValidationError, List[HolidayRequest]] =
    for {
      _ <- dateValidator
        .isDateInTheFuture(fromDate.atStartOfDay())

      _ <- dateValidator
        .areDatesInProperOrder(fromDate.atStartOfDay(), toDate.atStartOfDay())

      holidayRequests <- EitherT
        .right[DateValidationError] {
          (for {
            date <- DateRangeGenerator[F].getDateStream(fromDate, toDate)
            holidayRequestOption <- Stream.eval(
              holidayRequestStore.deleteUserHolidayRequestForDate(user.id, date).value,
            )
            holidayRequest <- Stream(holidayRequestOption.toSeq: _*)
          } yield holidayRequest)
            .compile
            .toList
        }
    } yield holidayRequests

  override def getAllHolidayRequests(): F[List[HolidayRequest]] =
    holidayRequestStore
      .getAll()

  override def getAllHolidayRequestsForSpecifiedDateRange(
    fromDate: LocalDate,
    toDate: LocalDate,
  ): EitherT[F, DateValidationError, List[HolidayRequest]] =
    for {
      _ <- dateValidator
        .areDatesInProperOrder(fromDate.atStartOfDay(), toDate.atStartOfDay())

      holidayRequests <- EitherT
        .right[DateValidationError] {
          holidayRequestStore.getAllBetweenDates(fromDate, toDate)
        }
    } yield holidayRequests

  override def getAllHolidayRequestsForUser(userId: UserId): F[List[HolidayRequest]] =
    holidayRequestStore.getAllForUser(userId)

  override def getAllPendingHolidayRequestsForUser(userId: UserId): F[List[HolidayRequest]] =
    holidayRequestStore.getAllPendingForUser(userId)

  override def getAllHolidayRequestsForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): EitherT[F, DateValidationError, List[HolidayRequest]] =
    for {
      _ <- dateValidator.areDatesInProperOrder(fromDate.atStartOfDay(), toDate.atStartOfDay())

      holidayRequests <- EitherT.right[DateValidationError] {
        holidayRequestStore.getAllForUserBetweenDates(userId, fromDate, toDate)
      }
    } yield holidayRequests

  override def getAllPendingHolidayRequestsForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): EitherT[F, DateValidationError, List[HolidayRequest]] =
    for {
      _ <- dateValidator
        .areDatesInProperOrder(fromDate.atStartOfDay(), toDate.atStartOfDay())

      holidayRequests <- EitherT.right[DateValidationError] {
        holidayRequestStore.getAllPendingForUserBetweenDates(userId, fromDate, toDate)
      }
    } yield holidayRequests

  private def createHolidayRequestEntity(
    userId: UserId,
    date: LocalDate,
    holidayType: HolidayType,
    description: String,
  ): HolidayRequest =
    HolidayRequest(
      ID.createNew(),
      userId,
      date,
      holidayType,
      description,
      Status.Pending,
    )
}

object HolidayRequestService {
  def apply[F[_]: Concurrent](
    dateValidator: DateValidatorAlgebra[F],
    holidayValidator: HolidayValidatorAlgebra[F],
    holidayRequestStore: HolidayRequestStoreAlgebra[F],
  ): HolidayRequestService[F] = new HolidayRequestService[F](dateValidator, holidayValidator, holidayRequestStore)
}
