package com.timesheet.core.validation
package holiday.impl

import java.time.{LocalDate, Year}

import cats._
import cats.data._
import cats.implicits._
import com.timesheet.core.store.holiday.HolidayStoreAlgebra
import com.timesheet.core.store.holidayrequest.HolidayRequestStoreAlgebra
import com.timesheet.core.validation.ValidationUtils._
import com.timesheet.core.validation.holiday.HolidayValidatorAlgebra
import com.timesheet.model.holiday.{Holiday, HolidayType}
import com.timesheet.model.holidayrequest.HolidayRequest
import com.timesheet.model.user.User

final class HolidayValidator[F[_]: Monad](
  holidayStore: HolidayStoreAlgebra[F],
  holidayRequestStore: HolidayRequestStoreAlgebra[F],
) extends HolidayValidatorAlgebra[F] {

  override def checkIfUserCanRequestHoliday(
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
    holidayType: HolidayType,
  ): EitherT[F, HolidayRequestValidationError, Unit] =
    for {
      countedDays <- EitherT.liftF {
        for {
          fromDateF       <- Year.of(fromDate.getYear).atDay(1).pure[F]
          toDateF         <- Year.of(toDate.getYear + 1).atDay(1).pure[F]
          holidays        <- holidayStore.getAllForUserBetweenDates(user.id, fromDateF, toDateF)
          holidayRequests <- holidayRequestStore.getAllPendingForUserBetweenDates(user.id, fromDateF, toDateF)
          holidayDays        = normalizeHolidaysAndGetDays(holidays, fromDateF, toDateF)
          holidayRequestDays = normalizeHolidayRequestsAndGetDays(holidayRequests, fromDateF, toDateF)
        } yield holidayDays + holidayRequestDays
      }
      requestedHolidayDays <- EitherT
        .rightT[F, HolidayRequestValidationError] {
          toDate.toEpochDay - fromDate.toEpochDay + 1
        }
      userHolidaysDays <- EitherT
        .rightT[F, HolidayRequestValidationError] {
          (toDate.getYear - fromDate.getYear + 1) * user.holidaysPerYear
        }
      _ <- EitherT.condUnit[F, HolidayRequestValidationError](
        countedDays + requestedHolidayDays <= userHolidaysDays,
        NotEnoughDaysForHolidays(
          requestedHolidayDays.toInt,
          (userHolidaysDays - countedDays).toInt,
        ),
      )
    } yield ()

  private def normalizeHolidaysAndGetDays(
    holidays: List[Holiday],
    fromDate: LocalDate,
    toDate: LocalDate,
  ): Long = {
    import com.timesheet.util.LocalDateTypeClassInstances._

    holidays
      .map { holiday =>
        holiday.copy(
          fromDate = holiday.fromDate.max(fromDate),
          toDate = holiday.toDate.min(toDate),
        )
      }
      .foldLeft(0L) {
        case (days, holiday) =>
          days + (holiday.toDate.toEpochDay - holiday.fromDate.toEpochDay + 1)
      }
  }

  private def normalizeHolidayRequestsAndGetDays(
    holidayRequests: List[HolidayRequest],
    fromDate: LocalDate,
    toDate: LocalDate,
  ): Long = {
    import com.timesheet.util.LocalDateTypeClassInstances._

    holidayRequests
      .map { holidayRequest =>
        holidayRequest.copy(
          fromDate = holidayRequest.fromDate.max(fromDate),
          toDate = holidayRequest.toDate.min(toDate),
        )
      }
      .foldLeft(0L) {
        case (days, holidayRequest) =>
          days + (holidayRequest.toDate.toEpochDay - holidayRequest.fromDate.toEpochDay + 1)
      }
  }
}

object HolidayValidator {
  def apply[F[_]: Monad](
    holidayStore: HolidayStoreAlgebra[F],
    holidayRequestStore: HolidayRequestStoreAlgebra[F],
  ): HolidayValidator[F] = new HolidayValidator[F](holidayStore, holidayRequestStore)
}
