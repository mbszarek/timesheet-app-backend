package com.timesheet.model.work

import java.time.Instant

import com.avsystem.commons.serialization.{GenCodec, HasGenCodec, HasGenCodecWithDeps, name}
import com.timesheet.model.db.ID
import com.timesheet.model.user.UserId
import com.timesheet.util.InstantTypeClassInstances

final case class WorkSample(
  @name("_id") id: ID,
  userId: UserId,
  activityType: ActivityType,
  date: Instant,
)
object WorkSample extends HasGenCodecWithDeps[InstantTypeClassInstances.type, WorkSample]
