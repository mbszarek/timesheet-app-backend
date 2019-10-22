package com.timesheet.core.store.user

import cats.data.OptionT
import com.timesheet.model.user.{User, UserId}

trait UserStoreAlgebra[F[_]] {
  def create(user: User): F[User]

  def update(user: User): OptionT[F, User]

  def get(userId: UserId): OptionT[F, User]

  def getAll(): F[List[User]]

  def delete(userId: UserId): OptionT[F, User]

  def findByUsername(username: String): OptionT[F, User]

  def deleteByUsername(username: String): OptionT[F, User]
}
