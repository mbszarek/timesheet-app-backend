package com.timesheet
package core.store.user.impl

import cats.effect._
import cats.data._
import cats.implicits._
import com.timesheet.core.db.MongoDriverMixin
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.model.user.{User, UserId}
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters._
import tsec.authentication.IdentityStore

final class UserStoreMongo[F[_]: ConcurrentEffect]
    extends UserStoreAlgebra[F]
    with IdentityStore[F, UserId, User]
    with MongoDriverMixin[F] {
  import User._

  override type T = User

  protected val collection: F[MongoCollection[User]] = getCollection("Users")

  override def create(user: User): F[User] =
    for {
      coll <- collection
      _    <- coll.insertOne(user).compileFS2.drain
    } yield user

  override def update(user: User): OptionT[F, User] =
    OptionT.liftF {
      for {
        coll <- collection
        _    <- coll.findOneAndReplace(idRef equal user.id, user).compileFS2.drain
      } yield user
    }

  override def delete(userId: UserId): OptionT[F, User] = OptionT {
    for {
      coll <- collection
      user <- coll.findOneAndDelete(idRef equal userId).compileFS2.last
    } yield user
  }

  override def findByUsername(username: String): OptionT[F, User] =
    OptionT {
      for {
        coll <- collection
        user <- coll.find(usernameRef equal username).compileFS2.last
      } yield user
    }

  override def deleteByUsername(username: String): OptionT[F, User] =
    for {
      user <- findByUsername(username)
      coll <- OptionT.liftF(collection)
      _    <- OptionT.liftF(coll.deleteOne(idRef equal user.id).compileFS2.drain)
    } yield user

  override def get(id: UserId): OptionT[F, User] =
    OptionT {
      for {
        coll <- collection
        user <- coll.find(idRef equal id).compileFS2.last
      } yield user
    }

  override def getAll(): F[List[User]] =
    for {
      coll     <- collection
      userList <- coll.find().compileFS2.toList
    } yield userList
}

object UserStoreMongo {
  def apply[F[_]: ConcurrentEffect]: UserStoreMongo[F] = new UserStoreMongo[F]()
}
