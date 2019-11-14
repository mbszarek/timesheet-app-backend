package com.timesheet.core.service.holiday.impl

import java.time.LocalDate

import cats.implicits._
import cats.data._
import cats.effect._
import com.timesheet.core.service.holiday.HolidayServiceAlgebra
import com.timesheet.core.store.holiday.HolidayStoreAlgebra
import com.timesheet.core.validation.ValidationUtils
import com.timesheet.core.validation.ValidationUtils.{HolidayNotFound, HolidayValidationError}
import com.timesheet.core.validation.holiday.HolidayValidatorAlgebra
import com.timesheet.model.holiday.{Holiday, HolidayType}
import com.timesheet.model.user.{User, UserId}
import com.timesheet.util.DateRangeGenerator
import fs2._

final class HolidayService[F[_]: Sync](
  holidayValidator: HolidayValidatorAlgebra[F],
  holidayStore: HolidayStoreAlgebra[F],
) extends HolidayServiceAlgebra[F] {

  override def createHoliday(
    user: User,
    date: LocalDate,
    holidayType: HolidayType,
  ): EitherT[F, ValidationUtils.HolidayValidationError, Holiday] =
    for {
      holiday <- holidayValidator.checkIfUserCanTakeHoliday(user, date, holidayType)
      _       <- EitherT.liftF(holidayStore.create(holiday))
    } yield holiday

  override def createHolidayForDateRange(
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
    holidayType: HolidayType,
  ): F[List[Either[ValidationUtils.HolidayValidationError, Holiday]]] =
    (for {
      date      <- DateRangeGenerator[F].getDateStream(fromDate, toDate)
      validated <- Stream.eval(holidayValidator.checkIfUserCanTakeHoliday(user, date, holidayType).value)

      _ <- validated match {
        case Left(_)      => Stream.empty
        case Right(value) => Stream.eval(holidayStore.create(value))
      }
    } yield validated).compile.toList

  override def deleteHoliday(
    user: User,
    date: LocalDate,
  ): EitherT[F, ValidationUtils.HolidayValidationError, Holiday] =
    EitherT {
      holidayStore
        .deleteUserHolidayForDate(user.id, date)
        .fold[Either[HolidayValidationError, Holiday]](
          HolidayNotFound(
            user,
            date,
          ).asLeft[Holiday],
        )(_.asRight[HolidayValidationError])
    }

  override def collectHolidaysForUser(userId: UserId): F[List[Holiday]] =
    holidayStore.getAllForUser(userId)

  override def collectHolidaysForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[List[Holiday]] =
    holidayStore.getAllForUserBetweenDates(userId, fromDate, toDate)
}

object HolidayService {
  def apply[F[_]: Sync](
    holidayValidator: HolidayValidatorAlgebra[F],
    holidayStore: HolidayStoreAlgebra[F],
  ): HolidayService[F] = new HolidayService[F](holidayValidator, holidayStore)
}
