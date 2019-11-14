package com.timesheet.model.work

import java.time.Instant

import com.avsystem.commons.serialization.{HasGenCodecWithDeps, name}
import com.timesheet.model.db.{DBEntityWithID, ID}
import com.timesheet.model.user.UserId
import com.timesheet.util.InstantTypeClassInstances

final case class WorkSample(
  @name("_id") id: ID,
  userId: UserId,
  activityType: ActivityType,
  date: Instant,
) extends DBEntityWithID

object WorkSample extends HasGenCodecWithDeps[InstantTypeClassInstances.type, WorkSample]
