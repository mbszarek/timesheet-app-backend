package com.timesheet.core.db

import com.timesheet.model.db.ID
import com.timesheet.model.user.User.UserId
import monix.eval.Task
import reactivemongo.bson.{BSONDocument, document}

trait MongoStoreUtils {
  protected def getIdSelector(id: ID): Task[BSONDocument] = Task.pure {
    import ID.idHandler

    document(
      "_id" -> id
    )
  }

  protected def getUserIdSelector(userId: UserId): Task[BSONDocument] = Task.pure {
    import UserId.userIdHandler

    document(
      "_id" -> userId
    )
  }
}
