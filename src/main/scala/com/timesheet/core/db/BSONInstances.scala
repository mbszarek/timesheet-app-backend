package com.timesheet.core.db

import java.time.Instant

import com.timesheet.model.user.User.UserId
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONReader, BSONString, BSONWriter, Macros}
import tsec.common.SecureRandomId

object BSONInstances {
  implicit val secureRandomIdWriter: BSONWriter[SecureRandomId, BSONString] =
    (id: SecureRandomId) => BSONString(id.toString)

  implicit val userIdWriter: BSONDocumentWriter[UserId] = Macros.writer[UserId]

  implicit val userIdReader: BSONDocumentReader[UserId] = Macros.reader[UserId]

  implicit val instantWriter: BSONWriter[Instant, BSONDateTime] = (t: Instant) => BSONDateTime(t.toEpochMilli)

  implicit val instantReader: BSONReader[BSONDateTime, Instant] = (bson: BSONDateTime) =>
    Instant.ofEpochMilli(bson.value)
}
