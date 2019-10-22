package com.timesheet.model.db

import com.avsystem.commons.serialization.{GenCodec, transparent}
import org.bson.types.ObjectId

@transparent
final case class ID(value: String)

object ID {
  implicit val Codec: GenCodec[ID] = GenCodec.materialize

  def createNew(): ID = ID(ObjectId.get().toHexString)
}
