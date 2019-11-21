package com.timesheet.core.service.holiday.impl

import java.time.LocalDate

import cats.data._
import cats.effect._
import cats.implicits._
import com.timesheet.core.service.base.EntityServiceAlgebra.EntityStore
import com.timesheet.core.service.base.impl.EntityServiceImpl
import com.timesheet.core.service.holiday.HolidayServiceAlgebra
import com.timesheet.core.store.holiday.HolidayStoreAlgebra
import com.timesheet.core.validation.ValidationUtils.{BasicError, DateValidationError, EntityNotFound, EntityRemovalError}
import com.timesheet.core.validation.date.DateValidatorAlgebra
import com.timesheet.core.validation.holiday.HolidayValidatorAlgebra
import com.timesheet.core.validation.user.UserValidatorAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.holiday.Holiday
import com.timesheet.model.user.{User, UserId}

final class HolidayService[F[_]: Sync](
  userValidator: UserValidatorAlgebra[F],
  dateValidator: DateValidatorAlgebra[F],
  holidayValidator: HolidayValidatorAlgebra[F],
  holidayStore: HolidayStoreAlgebra[F],
) extends EntityServiceImpl[F]
    with HolidayServiceAlgebra[F] {

  override protected def entityStore: EntityStore[F, Holiday] = holidayStore

  override def deleteHoliday(
    user: User,
    id: ID,
  ): EitherT[F, BasicError, Holiday] =
    for {
      holiday <- holidayStore
        .get(id)
        .toRight[BasicError](EntityNotFound(id))

      _ <- userValidator
        .canModifyResource(user, holiday)
        .leftWiden[BasicError]

      _ <- holidayStore
        .delete(id)
        .toRight[BasicError](EntityRemovalError(id))
    } yield holiday

  override def collectHolidaysForUser(userId: UserId): F[List[Holiday]] =
    holidayStore.getAllForUser(userId)

  override def collectHolidaysForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): EitherT[F, DateValidationError, List[Holiday]] =
    for {
      _ <- dateValidator
        .areDatesInProperOrder(fromDate.atStartOfDay(), toDate.atStartOfDay())

      holidays <- EitherT
        .right[DateValidationError] {
          holidayStore
            .getAllForUserBetweenDates(userId, fromDate, toDate)
        }
    } yield holidays
}

object HolidayService {
  def apply[F[_]: Sync](
    userValidator: UserValidatorAlgebra[F],
    dateValidator: DateValidatorAlgebra[F],
    holidayValidator: HolidayValidatorAlgebra[F],
    holidayStore: HolidayStoreAlgebra[F],
  ): HolidayService[F] = new HolidayService[F](userValidator, dateValidator, holidayValidator, holidayStore)
}
