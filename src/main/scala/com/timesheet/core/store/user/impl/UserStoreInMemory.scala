package com.timesheet.core.store.user.impl

import cats._
import cats.data.OptionT
import cats.implicits._
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.model.user.{User, UserId}
import tsec.authentication.IdentityStore

import scala.collection.concurrent.TrieMap

class UserStoreInMemory[F[_]: Applicative] extends UserStoreAlgebra[F] with IdentityStore[F, UserId, User] {
  private val cache = new TrieMap[UserId, User]

  def create(user: User): F[User] = {
    cache += (user.id -> user)
    user.pure[F]
  }

  def update(user: User): OptionT[F, User] = OptionT.liftF {
    cache.update(user.id, user)
    user.pure[F]
  }

  def get(userId: UserId): OptionT[F, User] = OptionT.fromOption(cache.get(userId))

  override def getAll(): F[List[User]] =
    cache.values.toList.pure[F]

  def delete(userId: UserId): OptionT[F, User] = OptionT.fromOption(cache.remove(userId))

  def findByUsername(username: String): OptionT[F, User] =
    OptionT.fromOption(cache.values.find(_.username === username))

  def deleteByUsername(username: String): OptionT[F, User] = OptionT.fromOption {
    for {
      user   <- cache.values.find(_.username === username)
      result <- cache.remove(user.id)
    } yield result
  }
}

object UserStoreInMemory {
  def apply[F[_]: Applicative] = new UserStoreInMemory[F]
}
