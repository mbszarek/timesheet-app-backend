package com.timesheet
package core.store.user.impl

import cats.data.OptionT
import cats.implicits._
import com.timesheet.concurrent.FutureConcurrentEffect
import com.timesheet.core.db.MongoDriverMixin
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters._
import tsec.authentication.IdentityStore

class UserStoreMongo[F[_]: FutureConcurrentEffect]
    extends UserStoreAlgebra[F]
    with IdentityStore[F, UserId, User]
    with MongoDriverMixin[F] {
  override type T = User

  protected val collection: F[MongoCollection[User]] = getCollection("Users")

  override def create(user: User): F[User] =
    for {
      coll <- collection
      _    <- coll.insertOne(user).toFS2.drain
    } yield user

  override def update(user: User): OptionT[F, User] =
    OptionT.liftF {
      for {
        coll <- collection
        _    <- coll.findOneAndReplace(equal("_id", user.id), user).toFS2.drain
      } yield user
    }

  override def delete(userId: UserId): OptionT[F, User] = OptionT {
    for {
      coll <- collection
      user <- coll.findOneAndDelete(equal("_id", userId)).toFS2.last
    } yield user
  }

  override def findByUsername(username: String): OptionT[F, User] =
    OptionT {
      for {
        coll <- collection
        user <- coll.find(equal("username", username)).toFS2.last
      } yield user
    }

  override def deleteByUsername(username: String): OptionT[F, User] =
    for {
      user <- findByUsername(username)
      coll <- OptionT.liftF(collection)
      _    <- OptionT.liftF(coll.deleteOne(equal("_id", user.id)).toFS2.drain)
    } yield user

  override def get(id: UserId): OptionT[F, User] =
    OptionT {
      for {
        coll <- collection
        user <- coll.find(equal("_id", id)).toFS2.last
      } yield user
    }

  override def getAll(): F[List[User]] =
    for {
      coll     <- collection
      userList <- coll.find().toFS2.toList
    } yield userList
}

object UserStoreMongo {
  def apply[F[_]: FutureConcurrentEffect]: UserStoreMongo[F] = new UserStoreMongo[F]()
}
