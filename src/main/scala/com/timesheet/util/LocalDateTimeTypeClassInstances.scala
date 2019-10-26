package com.timesheet.util

import java.time.LocalDateTime

import cats._

object LocalDateTimeTypeClassInstances {
  implicit val localDateTimeOrderInstance: Order[LocalDateTime] = Order.from[LocalDateTime](_ compareTo _)
}
