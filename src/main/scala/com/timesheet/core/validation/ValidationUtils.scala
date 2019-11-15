package com.timesheet.core.validation

import java.time.LocalDate

import com.timesheet.model.user.User

object ValidationUtils {
  sealed trait UserValidationError               extends Product with Serializable
  final case class UserAlreadyExists(user: User) extends UserValidationError
  final case object UserDoesNotExists            extends UserValidationError

  sealed trait WorkSampleValidationError extends Product with Serializable
  final case object WrongUserState       extends WorkSampleValidationError

  sealed trait DateValidationError  extends Product with Serializable
  final case object DateInTheFuture extends DateValidationError

  sealed trait HolidayValidationError extends Product with Serializable
  final case class HolidayNotFound(
    user: User,
    date: LocalDate,
  ) extends HolidayValidationError

  sealed trait HolidayRequestValidationError extends Product with Serializable
  final case class HolidayRequestNotFound(
    user: User,
    date: LocalDate,
  ) extends HolidayRequestValidationError
  final case class NotEnoughDaysForHolidays(daysLeft: Int) extends HolidayRequestValidationError

}
