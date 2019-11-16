package com.timesheet.core.service.user

import com.timesheet.model.user.{User, UserId}
import cats.data._
import com.timesheet.core.validation.ValidationUtils.UserAlreadyExists
import com.timesheet.core.validation.ValidationUtils._

trait UserServiceAlgebra[F[_]] {
  def create(user: User): EitherT[F, UserValidationError, User]

  def update(user: User): EitherT[F, UserValidationError, User]

  def getUserByUserId(userId: UserId): EitherT[F, UserValidationError, User]

  def getUserByUsername(username: String): EitherT[F, UserValidationError, User]

  def deleteByUsername(username: String): EitherT[F, UserValidationError, User]

  def delete(userId: UserId): EitherT[F, UserValidationError, User]

  def getAll(): F[List[User]]
}
