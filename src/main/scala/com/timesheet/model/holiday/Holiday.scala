package com.timesheet.model.holiday

import java.time.LocalDate

import com.avsystem.commons.serialization.{GenCodec, HasGenCodecWithDeps, name}
import com.timesheet.model.db.{DBEntityWithID, ID}
import com.timesheet.model.user.UserId
import com.timesheet.util.LocalDateTypeClassInstances

final case class Holiday(
  @name("_id") id: ID,
  userId: UserId,
  date: LocalDate,
  holidayType: HolidayType,
) extends DBEntityWithID

object Holiday extends HasGenCodecWithDeps[LocalDateTypeClassInstances.type, Holiday]