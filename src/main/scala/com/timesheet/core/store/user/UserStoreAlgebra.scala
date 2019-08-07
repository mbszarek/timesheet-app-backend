package com.timesheet.core.store.user

import cats.data.OptionT
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId

trait UserStoreAlgebra[F[_]] {
  def create(user: User): F[User]

  def update(user: User): OptionT[F, User]

  def get(userId: UserId): OptionT[F, User]

  def delete(userId: UserId): OptionT[F, User]

  def findByUserName(userName: String): OptionT[F, User]

  def deleteByUserName(userName: String): OptionT[F, User]
}
