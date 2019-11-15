package com.timesheet.core.service.holidayrequest.impl

import java.time.LocalDate

import cats.data._
import cats.effect._
import cats.effect.concurrent.MVar
import cats.implicits._
import com.timesheet.core.service.holidayrequest.HolidayRequestServiceAlgebra
import com.timesheet.core.store.holidayrequest.HolidayRequestStoreAlgebra
import com.timesheet.core.validation.ValidationUtils.{HolidayRequestNotFound, HolidayRequestValidationError}
import com.timesheet.core.validation.holiday.HolidayValidatorAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.holiday.HolidayType
import com.timesheet.model.holidayrequest.{HolidayRequest, Status}
import com.timesheet.model.user.{User, UserId}
import com.timesheet.util.DateRangeGenerator
import fs2._

final class HolidayRequestService[F[_]: Concurrent](
  holidayValidator: HolidayValidatorAlgebra[F],
  holidayRequestStore: HolidayRequestStoreAlgebra[F],
) extends HolidayRequestServiceAlgebra[F] {

  override def createHolidayRequest(
    user: User,
    date: LocalDate,
    holidayType: HolidayType,
    description: String,
  ): EitherT[F, HolidayRequestValidationError, HolidayRequest] =
    for {
      _ <- holidayValidator.checkIfUserCanRequestHoliday(user, date, 1, holidayType)
      result <- EitherT.liftF(
        holidayRequestStore.create(createHolidayRequestEntity(user.id, date, holidayType, description)),
      )
    } yield result

  override def createHolidayRequestForDateRange(
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
    holidayType: HolidayType,
    description: String,
  ): EitherT[F, HolidayRequestValidationError, List[HolidayRequest]] =
    for {
      dates <- EitherT.liftF(DateRangeGenerator[F].getDateRange(fromDate, toDate))

      _ <- holidayValidator
        .checkIfUserCanRequestHoliday(user, dates.head, dates.size, holidayType)

      result <- EitherT.liftF {
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
  ): EitherT[F, HolidayRequestValidationError, HolidayRequest] =
    EitherT {
      holidayRequestStore
        .deleteUserHolidayRequestForDate(user.id, date)
        .fold[Either[HolidayRequestValidationError, HolidayRequest]](
          HolidayRequestNotFound(
            user,
            date,
          ).asLeft[HolidayRequest],
        )(_.asRight[HolidayRequestValidationError])
    }

  override def getAllHolidayRequests(): F[List[HolidayRequest]] =
    holidayRequestStore
      .getAll()

  override def getAllHolidayRequestsForSpecifiedDateRange(
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[List[HolidayRequest]] = holidayRequestStore.getAllBetweenDates(fromDate, toDate)

  override def collectHolidayRequestsForUser(userId: UserId): F[List[HolidayRequest]] =
    holidayRequestStore.getAllForUser(userId)

  override def collectHolidayRequestsForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[List[HolidayRequest]] = holidayRequestStore.getAllForUserBetweenDates(userId, fromDate, toDate)

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
    holidayValidator: HolidayValidatorAlgebra[F],
    holidayRequestStore: HolidayRequestStoreAlgebra[F],
  ): HolidayRequestService[F] = new HolidayRequestService[F](holidayValidator, holidayRequestStore)
}
