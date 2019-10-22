package com.timesheet.model.worksample

import com.avsystem.commons.serialization.GenCodec

sealed trait ActivityType
case object Entrance  extends ActivityType
case object Departure extends ActivityType

object ActivityType {
  implicit val Codec: GenCodec[ActivityType] = GenCodec.materialize
}
