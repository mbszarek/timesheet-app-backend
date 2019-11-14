package com.timesheet.model.holidayrequest

import com.avsystem.commons.serialization.{HasGenCodec, transientDefault}
import com.timesheet.model.user.UserId

sealed trait Status extends Product with Serializable

object Status extends HasGenCodec[Status] {
  case object Pending extends Status
  final case class Denied(
    userId: UserId,
    @transientDefault reason: Option[String] = Option.empty,
  ) extends Status
}
