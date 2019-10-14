package com.timesheet
package core.store.user.impl

import cats.data.OptionT
import cats.implicits._
import com.timesheet.concurrent.FutureConcurrentEffect
import com.timesheet.core.db.MongoDriverMixin
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId
import reactivemongo.api.Cursor
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, document}
import tsec.authentication.IdentityStore

class UserStoreMongo[F[_]: FutureConcurrentEffect]
    extends UserStoreAlgebra[F]
    with IdentityStore[F, UserId, User]
    with MongoDriverMixin[F] {
  protected val collection: F[BSONCollection] = getCollection("Users")

  override def create(user: User): F[User] = {
    import User.userHandler

    for {
      _ <- collection.executeOnCollection(implicit sc => _.insert.one(user))
    } yield user
  }

  override def update(user: User): OptionT[F, User] =
    OptionT.liftF {
      import User.userHandler

      for {
        selector <- getUserIdSelector(user.id)
        x        <- collection.executeOnCollection(implicit sc => _.find(selector, None).one)
        _        <- collection.executeOnCollection(implicit sc => _.update.one(selector, user))
      } yield user
    }

  override def delete(userId: UserId): OptionT[F, User] = OptionT {
    {
      import User.userHandler

      for {
        selector <- getUserIdSelector(userId)
        user     <- collection.executeOnCollection[Option[User]](implicit sc => _.find(selector, None).one)
        _        <- collection.executeOnCollection[Int](implicit sc => _.delete.element(user.get).map(_.limit))
      } yield user
    }
  }

  override def findByUsername(username: String): OptionT[F, User] =
    OptionT {
      import User.userHandler

      for {
        selector <- getUsernameSelector(username)
        user     <- collection.executeOnCollection(implicit sc => _.find(selector, None).one)
      } yield user
    }

  override def deleteByUsername(username: String): OptionT[F, User] = {
    import User.userHandler

    for {
      user <- findByUsername(username)
      _ <- OptionT.liftF {
        collection.executeOnCollection(implicit sc => _.delete.one(user))
      }
    } yield user
  }

  override def get(id: UserId): OptionT[F, User] =
    OptionT {
      import User.userHandler

      for {
        selector <- getUserIdSelector(id)
        user     <- collection.executeOnCollection(implicit sc => _.find(selector, None).one)
      } yield user
    }

  override def getAll(): F[List[User]] = {
    import User.userHandler

    collection.executeOnCollection { implicit sc =>
      _.findList[User](document())
    }
  }

  private def getUserIdSelector(userId: UserId): F[BSONDocument] = {
    import com.timesheet.model.user.User.UserId.userIdHandler

    document(
      "_id" -> userId,
    )
  }.pure[F]

  private def getUsernameSelector(username: String): F[BSONDocument] = {
    document(
      "username" -> username,
    )
  }.pure[F]
}

object UserStoreMongo {
  def apply[F[_]: FutureConcurrentEffect]: UserStoreMongo[F] = new UserStoreMongo[F]()
}
