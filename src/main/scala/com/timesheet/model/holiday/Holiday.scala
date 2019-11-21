package com.timesheet.model.holiday

import java.time.LocalDate

import cats.effect.Sync
import com.avsystem.commons.mongo.BsonRef
import com.avsystem.commons.serialization.{HasGenCodecWithDeps, name}
import com.timesheet.model.db.{DBEntityWithID, DBEntityWithIDCompanion, DBEntityWithUserId, ID}
import com.timesheet.model.user.UserId
import com.timesheet.util.LocalDateTypeClassInstances
import org.http4s.circe._
import io.circe.generic.auto._
import org.http4s.EntityEncoder

final case class Holiday(
  @name("_id") id: ID,
  userId: UserId,
  fromDate: LocalDate,
  toDate: LocalDate,
  holidayType: HolidayType,
) extends DBEntityWithID
    with DBEntityWithUserId

object Holiday
    extends HasGenCodecWithDeps[LocalDateTypeClassInstances.type, Holiday]
    with DBEntityWithIDCompanion[Holiday] {
  import LocalDateTypeClassInstances.Codec

  implicit def encoder[F[_]: Sync]: EntityEncoder[F, Holiday] = jsonEncoderOf

  implicit val idRef: BsonRef[Holiday, ID]     = bson.ref(_.id)
  val userIdRef: BsonRef[Holiday, UserId]      = bson.ref(_.userId)
  val fromDateRef: BsonRef[Holiday, LocalDate] = bson.ref(_.fromDate)
  val toDateRef: BsonRef[Holiday, LocalDate]   = bson.ref(_.toDate)
}
