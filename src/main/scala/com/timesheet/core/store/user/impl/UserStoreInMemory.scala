package com.timesheet.core.store.user.impl

import cats._
import cats.data.OptionT
import cats.implicits._
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.model.user.{Role, User}
import com.timesheet.model.user.User.UserId
import tsec.authentication.IdentityStore

import scala.collection.concurrent.TrieMap
import scala.util.Random

class UserStoreInMemory[F[_]: Applicative] extends UserStoreAlgebra[F] with IdentityStore[F, UserId, User] {
  private val cache = new TrieMap[UserId, User]

  private val random = new Random()

  locally {
    cache += (UserId(1234) -> User(UserId(1234).some, "mszarek", "Mateusz", "Szarek", "a@a.com", "123", "123", Role.Admin))
  }

  def create(user: User): F[User] = {
    val id     = UserId(random.nextLong())
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

  def findByUserName(userName: String): OptionT[F, User] =
    OptionT.fromOption(cache.values.find(_.userName == userName))

  def deleteByUserName(userName: String): OptionT[F, User] = OptionT.fromOption {
    for {
      user   <- cache.values.find(_.userName == userName)
      userId <- user.id
      result <- cache.remove(userId)
    } yield result
  }
}

object UserStoreInMemory {
  def apply[F[_]: Applicative] = new UserStoreInMemory[F]
}
