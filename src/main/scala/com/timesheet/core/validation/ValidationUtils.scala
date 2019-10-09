package com.timesheet.core.validation

import com.timesheet.model.user.User

object ValidationUtils {
  sealed trait UserValidationError               extends Product with Serializable
  final case class UserAlreadyExists(user: User) extends UserValidationError
  final case object UserDoesNotExists            extends UserValidationError

  sealed trait WorkSampleValidationError extends Product with Serializable
  final case object WrongUserState       extends WorkSampleValidationError
}
