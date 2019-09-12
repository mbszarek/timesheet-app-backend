package com.timesheet
package core.store.user.impl

import cats.data.OptionT
import com.timesheet.core.db.MongoDriverMixin
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.model.user.User.UserId
import com.timesheet.model.user.{Role, User}
import monix.eval.Task
import reactivemongo.api.Cursor
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDocumentHandler, BSONDocumentReader, BSONDocumentWriter, Macros, document}
import tsec.authentication.IdentityStore

class UserStoreMongo extends UserStoreAlgebra[Task] with IdentityStore[Task, UserId, User] with MongoDriverMixin {
  protected val collection: Task[BSONCollection] = getCollection("Users")

  override def create(user: User): Task[User] = {
    import UserStoreMongo.UserInstances._

    for {
      _ <- collection.executeOnCollection(coll => implicit sc => coll.insert.one(user))
    } yield user
  }

  override def update(user: User): OptionT[Task, User] =
    OptionT {
      import UserStoreMongo.UserInstances._

      user.id.fold[Task[Option[User]]](Task.eval(None)) { userId =>
        for {
          selector <- getIdSelector(userId)
          _        <- collection.executeOnCollection(coll => implicit sc => coll.update.one(selector, user))
        } yield Some(user)
      }
    }

  override def delete(userId: UserId): OptionT[Task, User] = OptionT {
    {
      import UserStoreMongo.UserInstances._

      for {
        selector <- getIdSelector(userId)
        user     <- collection.executeOnCollection[Option[User]](coll => implicit sc => coll.find(selector, None).one)
        _        <- collection.executeOnCollection[Int](coll => implicit sc => coll.delete.element(user.get).map(_.limit))
      } yield user
    }
  }

  override def findByUsername(username: String): OptionT[Task, User] =
    OptionT {
      import UserStoreMongo.UserInstances._

      for {
        selector <- getUsernameSelector(username)
        user     <- collection.executeOnCollection(coll => implicit sc => coll.find(selector, None).one)
      } yield user
    }

  override def deleteByUsername(username: String): OptionT[Task, User] = {
    import UserStoreMongo.UserInstances._

    for {
      user <- findByUsername(username)
      _ <- OptionT.liftF {
        collection.executeOnCollection(coll => implicit sc => coll.delete.one(user))
      }
    } yield user
  }

  override def get(id: UserId): OptionT[Task, User] =
    OptionT {
      import UserStoreMongo.UserInstances._

      for {
        selector <- getIdSelector(id)
        user     <- collection.executeOnCollection(coll => implicit sc => coll.find(selector, None).one)
      } yield user
    }

  override def getAll(): Task[Seq[User]] = {
    import UserStoreMongo.UserInstances._

    collection.executeOnCollection(
      coll =>
        implicit sc => coll.find(BSONDocument(), None).cursor[User]().collect[Seq](-1, Cursor.FailOnError[Seq[User]]())
    )
  }

  private def getIdSelector(userId: UserId): Task[BSONDocument] = Task.pure {
    import com.timesheet.core.db.BSONInstances.mongoIdUserIdHandler

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

  implicit val roleHandler: BSONDocumentHandler[Role] = Macros.handler[Role]

  object UserInstances {
    import com.timesheet.core.db.BSONInstances.mongoIdUserIdHandler

    implicit val userReader: BSONDocumentReader[User] = Macros.reader[User]
    implicit val userWriter: BSONDocumentWriter[User] = Macros.writer[User]
  }
}
