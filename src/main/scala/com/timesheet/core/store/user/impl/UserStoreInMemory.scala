package com.timesheet.core.store.user.impl

import java.util.UUID

import cats._
import cats.data.OptionT
import cats.implicits._
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId
import tsec.authentication.IdentityStore

import scala.collection.concurrent.TrieMap

class UserStoreInMemory[F[_]: Applicative] extends UserStoreAlgebra[F] with IdentityStore[F, UserId, User] {
  private val cache = new TrieMap[UserId, User]

  def create(user: User): F[User] = {
    val id     = UserId(UUID.randomUUID().toString)
    val toSave = user.copy(id = id.some)
    cache += (id -> toSave)
    toSave.pure[F]
  }

  def update(user: User): OptionT[F, User] = OptionT {
    user.id.traverse { id =>
      cache.update(id, user)
      user.pure[F]
    }
  }

  def get(userId: UserId): OptionT[F, User] = OptionT.fromOption(cache.get(userId))

  def delete(userId: UserId): OptionT[F, User] = OptionT.fromOption(cache.remove(userId))

  def findByUsername(username: String): OptionT[F, User] =
    OptionT.fromOption(cache.values.find(_.username == username))

  def deleteByUsername(username: String): OptionT[F, User] = OptionT.fromOption {
    for {
      user   <- cache.values.find(_.username == username)
      userId <- user.id
      result <- cache.remove(userId)
    } yield result
  }
}

object UserStoreInMemory {
  def apply[F[_]: Applicative] = new UserStoreInMemory[F]
}
