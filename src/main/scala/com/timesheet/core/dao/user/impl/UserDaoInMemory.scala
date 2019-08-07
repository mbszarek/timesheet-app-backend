package com.timesheet.core.dao.user.impl

import cats._
import cats.data.OptionT
import cats.implicits._
import com.timesheet.core.dao.user.UserDaoAlgebra
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId
import tsec.authentication.IdentityStore

import scala.collection.concurrent.TrieMap
import scala.util.Random

class UserDaoInMemory[F[_]: Applicative] extends UserDaoAlgebra[F] with IdentityStore[F, UserId, User] {
  private val cache = new TrieMap[UserId, User]

  private val random = new Random()

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

object UserDaoInMemory {
  def apply[F[_]: Applicative] = new UserDaoInMemory[F]
}
