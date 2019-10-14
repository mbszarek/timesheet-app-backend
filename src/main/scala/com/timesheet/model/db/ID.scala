package com.timesheet.model.db

import com.avsystem.commons.serialization.{GenCodec, transparent}
import javax.xml.bind.DatatypeConverter
import reactivemongo.bson.{BSONHandler, BSONObjectID}

@transparent
final case class ID(value: String)

object ID {
  implicit val Codec: GenCodec[ID] = GenCodec.materialize

  def createNew(): ID = ID(BSONObjectID.generate().stringify)

  implicit val idHandler: BSONHandler[BSONObjectID, ID] = new BSONHandler[BSONObjectID, ID] {
    override def read(bson: BSONObjectID): ID = ID(bson.stringify)

    override def write(t: ID): BSONObjectID = BSONObjectID(DatatypeConverter.parseHexBinary(t.value))
  }
}
