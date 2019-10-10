package com.timesheet.core.db

import cats._
import cats.implicits._
import com.timesheet.model.db.ID
import com.timesheet.model.user.User.UserId
import reactivemongo.bson.{BSONDocument, document}

trait MongoStoreUtils[F[_]] {
  protected def getIdSelector(id: ID)(implicit F: Monad[F]): F[BSONDocument] = {
    import ID.idHandler

    document(
      "_id" -> id
    )
  }.pure[F]

  protected def getUserIdSelector(userId: UserId)(implicit F: Monad[F]): F[BSONDocument] = {
    import UserId.userIdHandler

    document(
      "_id" -> userId
    )
  }.pure[F]
}
