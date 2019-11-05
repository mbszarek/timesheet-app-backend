package com.timesheet.model.db

import com.avsystem.commons.serialization.{GenCodec, HasGenCodec, transparent}
import org.bson.types.ObjectId

@transparent
final case class ID(value: String)

object ID extends HasGenCodec[ID] {
  def createNew(): ID = ID(ObjectId.get().toHexString)
}
