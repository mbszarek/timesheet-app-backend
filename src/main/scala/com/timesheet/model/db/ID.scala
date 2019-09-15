package com.timesheet.model.db

import javax.xml.bind.DatatypeConverter
import reactivemongo.bson.{BSONHandler, BSONObjectID}

final case class ID(
  value: String
)

object ID {
  def createNew(): ID = ID(BSONObjectID.generate().stringify)

  implicit val idHandler: BSONHandler[BSONObjectID, ID] = new BSONHandler[BSONObjectID, ID] {
    override def read(bson: BSONObjectID): ID = ID(bson.stringify)

    override def write(t: ID): BSONObjectID = BSONObjectID(DatatypeConverter.parseHexBinary(t.value))
  }
}
