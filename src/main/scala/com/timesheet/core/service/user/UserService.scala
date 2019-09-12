package com.timesheet.core.service.user

import cats._
import cats.implicits._
import cats.data._
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.core.validation.ValidationUtils.{UserAlreadyExists, UserDoesNotExists}
import com.timesheet.core.validation.user.UserValidatorAlgebra
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId

class UserService[F[_]](userStore: UserStoreAlgebra[F], userValidator: UserValidatorAlgebra[F]) {
  def create(user: User)(implicit M: Monad[F]): EitherT[F, UserAlreadyExists, User] =
    for {
      _      <- userValidator.doesNotExist(user)
      result <- EitherT.liftF(userStore.create(user))
    } yield result

  def update(user: User)(implicit M: Monad[F]): EitherT[F, UserDoesNotExists.type, User] =
    for {
      _      <- userValidator.doesExist(user)
      result <- userStore.update(user).toRight(UserDoesNotExists)
    } yield result

  def getUserByUserId(userId: UserId)(implicit F: Functor[F]): EitherT[F, UserDoesNotExists.type, User] =
    userStore.get(userId).toRight(UserDoesNotExists)

  def getUserByUserName(username: String)(implicit F: Functor[F]): EitherT[F, UserDoesNotExists.type, User] =
    userStore.findByUsername(username).toRight(UserDoesNotExists)

  def deleteByUserName(username: String)(implicit F: Functor[F]): EitherT[F, UserDoesNotExists.type, User] =
    userStore.deleteByUsername(username).toRight(UserDoesNotExists)

  def delete(userId: UserId)(implicit F: Functor[F]): EitherT[F, UserDoesNotExists.type, User] =
    userStore.delete(userId).toRight(UserDoesNotExists)

  def getAll(): F[Seq[User]] =
    userStore.getAll()
}

object UserService {
  def apply[F[_]](userStore: UserStoreAlgebra[F], userValidator: UserValidatorAlgebra[F]): UserService[F] =
    new UserService[F](userStore, userValidator)
}
