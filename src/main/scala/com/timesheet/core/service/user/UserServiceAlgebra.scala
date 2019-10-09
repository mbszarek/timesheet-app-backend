package com.timesheet.core.service.user

import com.timesheet.model.user.User
import cats.data._
import com.timesheet.core.validation.ValidationUtils.UserAlreadyExists
import com.timesheet.core.validation.ValidationUtils._
import com.timesheet.model.user.User.UserId

trait UserServiceAlgebra[F[_]] {
  def create(user: User): EitherT[F, UserAlreadyExists, User]

  def update(user: User): EitherT[F, UserDoesNotExists.type, User]

  def getUserByUserId(userId: UserId): EitherT[F, UserDoesNotExists.type, User]

  def getUserByUsername(username: String): EitherT[F, UserDoesNotExists.type, User]

  def deleteByUsername(username: String): EitherT[F, UserDoesNotExists.type, User]

  def delete(userId: UserId): EitherT[F, UserDoesNotExists.type, User]

  def getAll(): F[Seq[User]]
}
