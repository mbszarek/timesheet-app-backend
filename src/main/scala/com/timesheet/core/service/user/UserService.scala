package com.timesheet.core.service.user

import cats._
import cats.implicits._
import cats.data._
import com.timesheet.core.dao.user.UserDaoAlgebra
import com.timesheet.core.validation.ValidationUtils.{UserAlreadyExists, UserDoesNotExists}
import com.timesheet.core.validation.user.UserValidatorAlgebra
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId

class UserService[F[_]](userDao: UserDaoAlgebra[F], userValidator: UserValidatorAlgebra[F]) {
  def create(user: User)(implicit M: Monad[F]): EitherT[F, UserAlreadyExists, User] =
    for {
      _      <- userValidator.doesNotExist(user)
      result <- EitherT.liftF(userDao.create(user))
    } yield result

  def update(user: User)(implicit M: Monad[F]): EitherT[F, UserDoesNotExists.type, User] =
    for {
      _      <- userValidator.doesExist(user)
      result <- userDao.update(user).toRight(UserDoesNotExists)
    } yield result

  def getUserByUserId(userId: UserId)(implicit F: Functor[F]): EitherT[F, UserDoesNotExists.type, User] =
    userDao.get(userId).toRight(UserDoesNotExists)

  def getUserByUserName(userName: String)(implicit F: Functor[F]): EitherT[F, UserDoesNotExists.type, User] =
    userDao.findByUserName(userName).toRight(UserDoesNotExists)

  def deleteByUserName(userName: String)(implicit F: Functor[F]): EitherT[F, UserDoesNotExists.type, User] =
    userDao.deleteByUserName(userName).toRight(UserDoesNotExists)

  def delete(userId: UserId)(implicit F: Functor[F]): EitherT[F, UserDoesNotExists.type, User] =
    userDao.delete(userId).toRight(UserDoesNotExists)
}

object UserService {
  def apply[F[_]](userDao: UserDaoAlgebra[F], userValidator: UserValidatorAlgebra[F]): UserService[F] =
    new UserService[F](userDao, userValidator)
}
