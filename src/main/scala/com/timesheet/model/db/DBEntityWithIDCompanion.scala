package com.timesheet.model.db

import com.avsystem.commons.mongo.BsonRef

trait DBEntityWithIDCompanion[T] extends DBEntityCompanion[T] {
  implicit val idRef: BsonRef[T, ID]
}
