package com.timesheet.service.user.impl

import cats.data._
import cats.effect._
import cats.implicits._
import com.timesheet.core.error.ValidationErrors.{UserDoesNotExists, UserValidationError}
import com.timesheet.service.user.UserServiceAlgebra
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.core.validation.user.UserValidatorAlgebra
import com.timesheet.model.user.{User, UserId}

final class UserService[F[_]: Sync](
  userStore: UserStoreAlgebra[F],
  userValidator: UserValidatorAlgebra[F])
    extends UserServiceAlgebra[F] {
  def create(user: User): EitherT[F, UserValidationError, User] =
    for {
      _ <- userValidator
        .doesNotExist(user)

      result <- EitherT
        .liftF(userStore.create(user))
    } yield result

  def update(user: User): EitherT[F, UserValidationError, User] =
    for {
      _ <- userValidator
        .doesExist(user)

      result <- userStore
        .update(user)
        .toRight(UserDoesNotExists)
        .leftWiden[UserValidationError]
    } yield result

  def getUserByUserId(userId: UserId): EitherT[F, UserValidationError, User] =
    userStore
      .get(userId)
      .toRight(UserDoesNotExists)
      .leftWiden[UserValidationError]

  def getUserByUsername(username: String): EitherT[F, UserValidationError, User] =
    userStore
      .findByUsername(username)
      .toRight(UserDoesNotExists)
      .leftWiden[UserValidationError]

  def deleteByUsername(username: String): EitherT[F, UserValidationError, User] =
    userStore
      .deleteByUsername(username)
      .toRight(UserDoesNotExists)
      .leftWiden[UserValidationError]

  def delete(userId: UserId): EitherT[F, UserValidationError, User] =
    userStore
      .delete(userId)
      .toRight(UserDoesNotExists)
      .leftWiden[UserValidationError]

  def getAll(): F[List[User]] =
    userStore
      .getAll()
}

object UserService {
  def apply[F[_]: Sync](
    userStore: UserStoreAlgebra[F],
    userValidator: UserValidatorAlgebra[F],
  ): UserService[F] =
    new UserService[F](userStore, userValidator)
}
