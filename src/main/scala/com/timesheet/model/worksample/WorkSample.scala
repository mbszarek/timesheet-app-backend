package com.timesheet.model.worksample

import java.time.Instant

import com.timesheet.model.db.ID
import com.timesheet.model.user.User.UserId
import reactivemongo.bson.Macros.Annotations.Key
import reactivemongo.bson.{BSONDocumentHandler, BSONObjectID, Macros}

final case class WorkSample(
  @Key("_id") id: ID,
  userId: UserId,
  activityType: ActivityType,
  date: Instant,
)

object WorkSample {
  import com.timesheet.core.db.BSONInstances.instantHandler

  implicit val workSampleHandler: BSONDocumentHandler[WorkSample] = Macros.handler[WorkSample]
}
