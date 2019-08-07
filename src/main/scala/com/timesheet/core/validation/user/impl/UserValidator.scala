package com.timesheet.core.validation.user.impl

import cats._
import cats.data._
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.core.validation.ValidationUtils
import com.timesheet.core.validation.ValidationUtils.{UserAlreadyExists, UserDoesNotExists}
import com.timesheet.core.validation.user.UserValidatorAlgebra
import com.timesheet.model.user.User

class UserValidator[F[_]: Applicative](userStore: UserStoreAlgebra[F]) extends UserValidatorAlgebra[F] {
  def doesExist(user: User): EitherT[F, ValidationUtils.UserDoesNotExists.type, Unit] =
    userStore
      .findByUserName(user.userName)
      .map(_ => ())
      .toRight(UserDoesNotExists)

  def doesNotExist(user: User): EitherT[F, ValidationUtils.UserAlreadyExists, Unit] =
    userStore
      .findByUserName(user.userName)
      .map(UserAlreadyExists.apply)
      .toLeft(())
}

object UserValidator {
  def apply[F[_]: Applicative](userStoreAlgebra: UserStoreAlgebra[F]): UserValidatorAlgebra[F] =
    new UserValidator[F](userStoreAlgebra)
}
