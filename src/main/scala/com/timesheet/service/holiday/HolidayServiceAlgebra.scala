package com.timesheet.service.holiday

import java.time.LocalDate

import cats.data._
import com.timesheet.core.error.ValidationErrors.{BasicError, DateValidationError}
import com.timesheet.service.base.EntityServiceAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.holiday.Holiday
import com.timesheet.model.user.{User, UserId}

trait HolidayServiceAlgebra[F[_]] extends EntityServiceAlgebra[F] {
  override type Entity = Holiday

  def deleteHoliday(
    user: User,
    id: ID,
  ): EitherT[F, BasicError, Holiday]

  def collectHolidaysForUser(userId: UserId): F[List[Holiday]]

  def collectHolidaysForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): EitherT[F, DateValidationError, List[Holiday]]
}
