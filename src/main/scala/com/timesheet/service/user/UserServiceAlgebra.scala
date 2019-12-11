package com.timesheet.service.user

import cats.data._
import com.timesheet.core.error.ValidationErrors._
import com.timesheet.model.user.{User, UserId}

trait UserServiceAlgebra[F[_]] {
  def create(user: User): EitherT[F, UserValidationError, User]

  def update(user: User): EitherT[F, UserValidationError, User]

  def getUserByUserId(userId: UserId): EitherT[F, UserValidationError, User]

  def getUserByUsername(username: String): EitherT[F, UserValidationError, User]

  def deleteByUsername(username: String): EitherT[F, UserValidationError, User]

  def delete(userId: UserId): EitherT[F, UserValidationError, User]

  def getAll(): F[List[User]]
}
