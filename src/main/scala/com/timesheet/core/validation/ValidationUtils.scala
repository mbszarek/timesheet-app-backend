package com.timesheet.core.validation

import java.time.LocalDate

import com.timesheet.model.db.ID
import com.timesheet.model.user.User

object ValidationUtils {
  sealed trait BasicError extends Product with Serializable

  sealed trait EntityError                    extends BasicError
  final case class EntityNotFound(id: ID)     extends EntityError
  final case class EntityRemovalError(id: ID) extends EntityError

  sealed trait DateValidationError extends BasicError
  case object DateInTheFuture      extends DateValidationError
  case object DateInThePast        extends DateValidationError
  case object WrongDateOrder       extends DateValidationError

  sealed trait UserValidationError               extends BasicError
  final case class UserAlreadyExists(user: User) extends UserValidationError
  case object UserDoesNotExists                  extends UserValidationError
  case object RestrictedAccess                   extends UserValidationError

  sealed trait WorkSampleValidationError extends DateValidationError
  final case object WrongUserState       extends WorkSampleValidationError

  sealed trait HolidayValidationError extends DateValidationError
  final case class HolidayNotFound(
    user: User,
    date: LocalDate,
  ) extends HolidayValidationError

  sealed trait HolidayRequestValidationError extends DateValidationError
  final case class HolidayRequestNotFound(
    user: User,
    date: LocalDate,
  ) extends HolidayRequestValidationError
  final case class NotEnoughDaysForHolidays(
    daysRequest: Int,
    daysLeft: Int,
  ) extends HolidayRequestValidationError
}
