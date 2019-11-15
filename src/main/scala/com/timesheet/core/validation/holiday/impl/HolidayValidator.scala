package com.timesheet.core.validation.holiday.impl

import java.time.{LocalDate, Year}

import cats._
import cats.implicits._
import cats.data._
import cats.effect._
import com.timesheet.core.store.holiday.HolidayStoreAlgebra
import com.timesheet.core.store.holidayrequest.HolidayRequestStoreAlgebra
import com.timesheet.core.validation.ValidationUtils
import com.timesheet.core.validation.ValidationUtils.{HolidayRequestValidationError, HolidayValidationError, NotEnoughDaysForHolidays}
import com.timesheet.core.validation.holiday.HolidayValidatorAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.holiday.{Holiday, HolidayType}
import com.timesheet.model.holidayrequest.{HolidayRequest, Status}
import com.timesheet.model.user.User

final class HolidayValidator[F[_]: Monad](
  holidayStore: HolidayStoreAlgebra[F],
  holidayRequestStore: HolidayRequestStoreAlgebra[F],
) extends HolidayValidatorAlgebra[F] {
  def checkIfUserCanTakeHoliday(
    user: User,
    date: LocalDate,
    holidayType: HolidayType,
  ): EitherT[F, ValidationUtils.HolidayValidationError, Holiday] =
    EitherT.fromEither[F] {
      Holiday(
        ID.createNew(),
        user.id,
        date,
        holidayType,
      ).asRight[HolidayValidationError]
    }

  def checkIfUserCanRequestHoliday(
    user: User,
    date: LocalDate,
    amountOfDays: Int,
    holidayType: HolidayType,
  ): EitherT[F, HolidayRequestValidationError, Unit] =
    for {
      holidaysCountedDays <- EitherT.liftF {
        for {
          fromDate <- Year.of(date.getYear).atDay(1).pure[F]
          toDate   <- Year.of(date.getYear + 1).atDay(1).pure[F]
          holidayCountInYear <- holidayStore
            .countForUserForDateRange(user.id, fromDate, toDate)
          holidayRequestsCountInYear <- holidayRequestStore
            .countForUserForDateRange(user.id, fromDate, toDate)
        } yield holidayCountInYear + holidayRequestsCountInYear
      }
      holidayRequest <- EitherT.cond[F](
        holidaysCountedDays + amountOfDays <= user.holidaysPerYear,
        (),
        NotEnoughDaysForHolidays(user.holidaysPerYear - holidaysCountedDays.toInt): HolidayRequestValidationError,
      )
    } yield holidayRequest
}

object HolidayValidator {
  def apply[F[_]: Monad](
    holidayStore: HolidayStoreAlgebra[F],
    holidayRequestStore: HolidayRequestStoreAlgebra[F],
  ): HolidayValidator[F] = new HolidayValidator[F](holidayStore, holidayRequestStore)
}
