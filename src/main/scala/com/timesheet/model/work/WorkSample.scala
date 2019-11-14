package com.timesheet.model.work

import java.time.Instant

import com.avsystem.commons.mongo.BsonRef
import com.avsystem.commons.serialization.{HasGenCodecWithDeps, name}
import com.timesheet.model.db.{DBEntityWithID, DBEntityWithIDCompanion, ID}
import com.timesheet.model.user.UserId
import com.timesheet.util.InstantTypeClassInstances

final case class WorkSample(
  @name("_id") id: ID,
  userId: UserId,
  activityType: ActivityType,
  date: Instant,
) extends DBEntityWithID

object WorkSample
    extends HasGenCodecWithDeps[InstantTypeClassInstances.type, WorkSample]
    with DBEntityWithIDCompanion[WorkSample] {
  import InstantTypeClassInstances.Codec

  implicit val idRef: BsonRef[WorkSample, ID] = bson.ref(_.id)
  val userIdRef: BsonRef[WorkSample, UserId]  = bson.ref(_.userId)
  val dateRef: BsonRef[WorkSample, Instant]   = bson.ref(_.date)
}
