package com.timesheet.core.validation

import com.timesheet.model.user.User

object ValidationUtils {
  sealed trait ValidationError                   extends Product with Serializable
  final case class UserAlreadyExists(user: User) extends ValidationError
  final case object UserDoesNotExists            extends ValidationError
}
