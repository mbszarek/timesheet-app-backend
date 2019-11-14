package com.timesheet.model.holiday

import java.time.LocalDate

import com.avsystem.commons.mongo.BsonRef
import com.avsystem.commons.serialization.{GenCodec, HasGenCodecWithDeps, name}
import com.timesheet.model.db.{DBEntityCompanion, DBEntityWithID, DBEntityWithIDCompanion, ID}
import com.timesheet.model.user.UserId
import com.timesheet.util.LocalDateTypeClassInstances

final case class Holiday(
  @name("_id") id: ID,
  userId: UserId,
  date: LocalDate,
  holidayType: HolidayType,
) extends DBEntityWithID

object Holiday
    extends HasGenCodecWithDeps[LocalDateTypeClassInstances.type, Holiday]
    with DBEntityWithIDCompanion[Holiday] {
  import LocalDateTypeClassInstances.Codec

  implicit val idRef: BsonRef[Holiday, ID] = bson.ref(_.id)
  val userIdRef: BsonRef[Holiday, UserId]  = bson.ref(_.userId)
  val dateRef: BsonRef[Holiday, LocalDate] = bson.ref(_.date)
}
