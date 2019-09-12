package com.timesheet.core.db

import java.time.Instant

import com.timesheet.model.user.User.UserId
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONDocumentHandler, BSONDocumentReader, BSONDocumentWriter, BSONHandler, BSONObjectID, BSONReader, BSONString, BSONWriter, Macros}
import tsec.common.SecureRandomId

object BSONInstances {
  implicit val instantHandler: BSONHandler[BSONDateTime, Instant] = new BSONHandler[BSONDateTime, Instant] {
    override def write(t: Instant): BSONDateTime = BSONDateTime(t.toEpochMilli)

    override def read(bson: BSONDateTime): Instant = Instant.ofEpochMilli(bson.value)
  }

  implicit val secureRandomIdWriter: BSONWriter[SecureRandomId, BSONString] =
    (id: SecureRandomId) => BSONString(id)

  implicit val mongoIdUserIdHandler: BSONHandler[BSONObjectID, UserId] = new BSONHandler[BSONObjectID, UserId] {
    override def write(id: UserId): BSONObjectID = BSONObjectID(id.id.getBytes())

    override def read(bson: BSONObjectID): UserId = UserId(bson.stringify)
  }

  implicit val userIdHandler: BSONDocumentHandler[UserId] = Macros.handler[UserId]
}
