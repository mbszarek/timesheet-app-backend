package com.timesheet.model.worksample

import cats._
import com.avsystem.commons.serialization.HasGenCodec

sealed trait ActivityType
case object Entrance  extends ActivityType
case object Departure extends ActivityType

object ActivityType extends HasGenCodec[ActivityType] {
  implicit val EqInstance: Eq[ActivityType] = Eq.instance[ActivityType](_ == _)
}
