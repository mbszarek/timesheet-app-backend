package com.timesheet.core.validation.user

import cats.data._
import com.timesheet.model.user.User

trait UserValidatorAlgebra[F[_]] {
  import com.timesheet.core.validation.ValidationUtils._

  def doesExist(user: User): EitherT[F, UserValidationError, Unit]

  def doesNotExist(user: User): EitherT[F, UserValidationError, Unit]
}
