package com.timesheet.core.db

import cats.Monad
import com.timesheet.model.db.ID
import com.timesheet.model.user.User.UserId
import monix.eval.Task
import reactivemongo.bson.{BSONDocument, document}

trait MongoStoreUtils[F[_]] {
  protected def getIdSelector(id: ID)(implicit F: Monad[F]): F[BSONDocument] = F.pure {
    import ID.idHandler

    document(
      "_id" -> id
    )
  }

  protected def getUserIdSelector(userId: UserId)(implicit F: Monad[F]): F[BSONDocument] = F.pure {
    import UserId.userIdHandler

    document(
      "_id" -> userId
    )
  }
}
