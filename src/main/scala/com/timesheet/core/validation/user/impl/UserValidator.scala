package com.timesheet.core.validation
package user.impl

import cats._
import cats.data._
import cats.implicits._
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.core.error.ValidationErrors.{UserAlreadyExists, UserDoesNotExists, UserValidationError, RestrictedAccess}
import com.timesheet.core.validation.user.UserValidatorAlgebra
import com.timesheet.model.db.DBEntityWithUserId
import com.timesheet.model.user.User

final class UserValidator[F[_]: Applicative](userStore: UserStoreAlgebra[F]) extends UserValidatorAlgebra[F] {
  def doesExist(user: User): EitherT[F, UserValidationError, Unit] =
    userStore
      .findByUsername(user.username)
      .map(_ => ())
      .toRight(UserDoesNotExists)
      .leftWiden[UserValidationError]

  def doesNotExist(user: User): EitherT[F, UserValidationError, Unit] =
    userStore
      .findByUsername(user.username)
      .map(UserAlreadyExists)
      .toLeft(())
      .leftWiden[UserValidationError]

  def canModifyResource(
    user: User,
    entity: DBEntityWithUserId,
  ): EitherT[F, UserValidationError, Unit] =
    EitherT
      .condUnit[F, UserValidationError](
        user.role.isAdmin || user.id === entity.userId,
        RestrictedAccess,
      )
}

object UserValidator {
  def apply[F[_]: Applicative](userStoreAlgebra: UserStoreAlgebra[F]): UserValidatorAlgebra[F] =
    new UserValidator[F](userStoreAlgebra)
}
