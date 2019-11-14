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
import com.timesheet.model.holiday.HolidayType
import com.timesheet.model.holidayrequest.HolidayRequest
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
      holidayRequest <- holidayValidator.checkIfUserCanRequestHoliday(user, date, 1, holidayType, description)
      _              <- EitherT.liftF(holidayRequestStore.create(holidayRequest))
    } yield holidayRequest

  override def createHolidayRequestForDateRange(
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
    holidayType: HolidayType,
    description: String,
  ): F[List[Either[HolidayRequestValidationError, HolidayRequest]]] =
    (for {
      mvar <- Stream.eval(MVar[F].of(0))
      date <- DateRangeGenerator[F].getDateStream(fromDate, toDate)

      validated <- Stream.eval {
        for {
          value <- mvar.take
          validated <- holidayValidator
            .checkIfUserCanRequestHoliday(user, date, value + 1, holidayType, description)
            .value
          _ <- mvar.put(value + 1)
        } yield validated
      }

      _ <- validated match {
        case Left(_)      => Stream.empty
        case Right(value) => Stream.eval(holidayRequestStore.create(value))
      }
    } yield validated).compile.toList

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
}

object HolidayRequestService {
  def apply[F[_]: Concurrent](
    holidayValidator: HolidayValidatorAlgebra[F],
    holidayRequestStore: HolidayRequestStoreAlgebra[F],
  ): HolidayRequestService[F] = new HolidayRequestService[F](holidayValidator, holidayRequestStore)
}
