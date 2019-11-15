package com.timesheet.model.work

import java.time.Instant

import com.avsystem.commons.mongo.BsonRef
import cats.effect.Sync
import com.avsystem.commons.serialization.{HasGenCodecWithDeps, name}
import com.timesheet.model.db.{DBEntityWithID, DBEntityWithIDCompanion, ID}
import com.timesheet.model.user.UserId
import com.timesheet.util.InstantTypeClassInstances
import io.circe.generic.auto._
import org.http4s.EntityEncoder
import org.http4s.circe._

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

  implicit def encoder[F[_]: Sync]: EntityEncoder[F, WorkSample] = jsonEncoderOf

  implicit val idRef: BsonRef[WorkSample, ID] = bson.ref(_.id)
  val userIdRef: BsonRef[WorkSample, UserId]  = bson.ref(_.userId)
  val dateRef: BsonRef[WorkSample, Instant]   = bson.ref(_.date)
}
