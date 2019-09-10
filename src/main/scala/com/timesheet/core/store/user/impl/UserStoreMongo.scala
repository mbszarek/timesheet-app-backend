package com.timesheet.core.store.user.impl

import cats.data.OptionT
import com.timesheet.core.db.MongoDriverMixin
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId
import monix.eval.Task
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}
import tsec.authentication.IdentityStore

class UserStoreMongo extends UserStoreAlgebra[Task] with IdentityStore[Task, UserId, User] with MongoDriverMixin {
  import com.timesheet.core.db.BSONInstances.userIdWriter
  import UserStoreMongo._

  protected val collection: Task[BSONCollection] = getCollection("Users")

  override def create(user: User): Task[User] =
    for {
      _ <- collection.flatMap { coll =>
        Task.deferFutureAction { implicit sc =>
          coll.insert.one(user)
        }
      }
    } yield user

  override def update(user: User): OptionT[Task, User] =
    OptionT.liftF(
      for {
        selector <- getIdSelector(user)
      _ <- collection.flatMap { coll =>
        Task.deferFutureAction { implicit sc =>
          coll.update.one(selector, user)
        }
      }
      } yield ???
    )

  override def delete(userId: UserId): OptionT[Task, User] = ???

  override def findByUserName(userName: String): OptionT[Task, User] = ???

  override def deleteByUserName(userName: String): OptionT[Task, User] = ???

  override def get(id: UserId): OptionT[Task, User] = ???

  private def getIdSelector(user: User): Task[BSONDocument] = Task.pure {
    document(
      "id" -> user.id.get
    )
  }
}

object UserStoreMongo {
  implicit val userReader: BSONDocumentReader[User] = Macros.reader[User]
  implicit val userWriter: BSONDocumentWriter[User] = Macros.writer[User]
}
