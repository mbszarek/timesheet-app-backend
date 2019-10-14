package com.timesheet.core.db

import java.time.Instant

import com.avsystem.commons.serialization.GenCodec
import com.timesheet.model.user.User.UserId
import javax.xml.bind.DatatypeConverter
import reactivemongo.bson.{BSONDateTime, BSONHandler, BSONObjectID, BSONString, BSONWriter}
import tsec.common.SecureRandomId

object BSONInstances {
  implicit val instantHandler: BSONHandler[BSONDateTime, Instant] = new BSONHandler[BSONDateTime, Instant] {
    override def write(t: Instant): BSONDateTime = BSONDateTime(t.toEpochMilli)

    override def read(bson: BSONDateTime): Instant = Instant.ofEpochMilli(bson.value)
  }

  implicit val secureRandomIdWriter: BSONWriter[SecureRandomId, BSONString] =
    (id: SecureRandomId) => BSONString(id)

  implicit val SecureRandomIdCodec: GenCodec[SecureRandomId] = GenCodec.createSimple[SecureRandomId]({ input =>
    input.readString().asInstanceOf[SecureRandomId]
  }, { (output, secureRandomId) =>
    output.writeString(secureRandomId)
  }, allowNull = false)

  implicit val
}
