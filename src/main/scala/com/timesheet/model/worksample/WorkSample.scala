package com.timesheet.model.worksample

import java.time.Instant

import com.avsystem.commons.serialization.{GenCodec, name}
import com.timesheet.model.db.ID
import com.timesheet.model.user.User.UserId

final case class WorkSample(
  @name("_id") id: ID,
  userId: UserId,
  activityType: ActivityType,
  date: Instant,
)

object WorkSample {
  import com.timesheet.util.InstantTypeClassInstances.{Codec => InstantCodec}

  implicit val Codec: GenCodec[WorkSample] = GenCodec.materialize
}
