package com.timesheet.model.worksample

import java.time.Instant

import com.avsystem.commons.serialization.{GenCodec, name}
import com.timesheet.model.db.ID
import com.timesheet.model.user.User.UserId
import reactivemongo.bson.Macros.Annotations.Key
import reactivemongo.bson.{BSONDocumentHandler, BSONObjectID, Macros}

final case class WorkSample(
  @Key("_id") @name("_id") id: ID,
  userId: UserId,
  activityType: ActivityType,
  date: Instant,
)

object WorkSample {
  import com.timesheet.core.db.BSONInstances.instantHandler
  import com.timesheet.util.InstantTypeClassInstances.{Codec => InstantCodec}

  implicit val Codec: GenCodec[WorkSample] = GenCodec.materialize

  implicit val workSampleHandler: BSONDocumentHandler[WorkSample] = Macros.handler[WorkSample]
}
