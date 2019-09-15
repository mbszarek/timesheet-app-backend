package com.timesheet
package core.store.user.impl

import cats.data.OptionT
import com.timesheet.core.db.MongoDriverMixin
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId
import monix.eval.Task
import reactivemongo.api.Cursor
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, document}
import tsec.authentication.IdentityStore

class UserStoreMongo extends UserStoreAlgebra[Task] with IdentityStore[Task, UserId, User] with MongoDriverMixin {
  protected val collection: Task[BSONCollection] = getCollection("Users")

  override def create(user: User): Task[User] = {
    import User.userHandler

    for {
      _ <- collection.executeOnCollection(implicit sc => _.insert.one(user))
    } yield user
  }

  override def update(user: User): OptionT[Task, User] =
    OptionT.liftF {
      import User.userHandler

      for {
        selector <- getUserIdSelector(user.id)
        _        <- collection.executeOnCollection(implicit sc => _.update.one(selector, user))
      } yield user
    }

  override def delete(userId: UserId): OptionT[Task, User] = OptionT {
    {
      import User.userHandler

      for {
        selector <- getUserIdSelector(userId)
        user     <- collection.executeOnCollection[Option[User]](implicit sc => _.find(selector, None).one)
        _        <- collection.executeOnCollection[Int](implicit sc => _.delete.element(user.get).map(_.limit))
      } yield user
    }
  }

  override def findByUsername(username: String): OptionT[Task, User] =
    OptionT {
      import User.userHandler

      for {
        selector <- getUsernameSelector(username)
        user     <- collection.executeOnCollection(implicit sc => _.find(selector, None).one)
      } yield user
    }

  override def deleteByUsername(username: String): OptionT[Task, User] = {
    import User.userHandler

    for {
      user <- findByUsername(username)
      _ <- OptionT.liftF {
        collection.executeOnCollection(implicit sc => _.delete.one(user))
      }
    } yield user
  }

  override def get(id: UserId): OptionT[Task, User] =
    OptionT {
      import User.userHandler

      for {
        selector <- getUserIdSelector(id)
        user     <- collection.executeOnCollection(implicit sc => _.find(selector, None).one)
      } yield user
    }

  override def getAll(): Task[Seq[User]] = {
    import User.userHandler

    collection.executeOnCollection { implicit sc =>
      _.find(BSONDocument(), None).cursor[User]().collect[Seq](-1, Cursor.FailOnError[Seq[User]]())
    }
  }

  private def getUserIdSelector(userId: UserId): Task[BSONDocument] = Task.pure {
    import com.timesheet.model.user.User.UserId.userIdHandler

    document(
      "_id" -> userId,
    )
  }

  private def getUsernameSelector(username: String): Task[BSONDocument] = Task.pure {
    document(
      "username" -> username,
    )
  }
}

object UserStoreMongo {
  def apply(): UserStoreMongo = new UserStoreMongo()
}
