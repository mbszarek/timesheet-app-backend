package com.timesheet.model.holiday

import com.avsystem.commons.serialization.{HasGenCodec, transparent}

@transparent
final case class HolidayType(value: String)

object HolidayType extends HasGenCodec[HolidayType]