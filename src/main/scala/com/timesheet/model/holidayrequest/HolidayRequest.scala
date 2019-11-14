package com.timesheet.model.holidayrequest

import java.time.LocalDate

import com.avsystem.commons.serialization.{HasGenCodecWithDeps, name}
import com.timesheet.model.db.{DBEntityWithID, ID}
import com.timesheet.model.holiday.{Holiday, HolidayType}
import com.timesheet.model.user.UserId
import com.timesheet.util.LocalDateTypeClassInstances

final case class HolidayRequest(
  @name("_id") id: ID,
  userId: UserId,
  date: LocalDate,
  holidayType: HolidayType,
  description: String,
  status: Status,
) extends DBEntityWithID {

  def toHoliday: Holiday = Holiday(
    ID.createNew(),
    userId,
    date,
    holidayType,
  )

  def deny(
    userId: UserId,
    reason: Option[String] = Option.empty,
  ): HolidayRequest = copy(status = Status.Denied(userId, reason))
}

object HolidayRequest extends HasGenCodecWithDeps[LocalDateTypeClassInstances.type, HolidayRequest]
