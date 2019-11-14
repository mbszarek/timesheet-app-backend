package com.timesheet.model.db

import com.avsystem.commons.mongo.BsonRef
import com.avsystem.commons.serialization.HasGenCodec

trait DBEntityCompanion[T] {
  protected def bson[K]: BsonRef.Creator[T] = BsonRef.create[T]
}
