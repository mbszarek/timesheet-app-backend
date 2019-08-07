package com.timesheet.core.validation.user.impl

import cats._
import cats.data._
import com.timesheet.core.dao.user.UserDaoAlgebra
import com.timesheet.core.validation.ValidationUtils
import com.timesheet.core.validation.ValidationUtils.{UserAlreadyExists, UserDoesNotExists}
import com.timesheet.core.validation.user.UserValidatorAlgebra
import com.timesheet.model.user.User

class UserValidator[F[_]: Applicative](userDao: UserDaoAlgebra[F]) extends UserValidatorAlgebra[F] {
  def doesExist(user: User): EitherT[F, ValidationUtils.UserDoesNotExists.type, Unit] =
    userDao
      .findByUserName(user.userName)
      .map(_ => ())
      .toRight(UserDoesNotExists)

  def doesNotExist(user: User): EitherT[F, ValidationUtils.UserAlreadyExists, Unit] =
    userDao
      .findByUserName(user.userName)
      .map(UserAlreadyExists.apply)
      .toLeft(())
}

object UserValidator {
  def apply[F[_]: Applicative](userDaoAlgebra: UserDaoAlgebra[F]): UserValidatorAlgebra[F] =
    new UserValidator[F](userDaoAlgebra)
}
