package com.timesheet.util

import java.time.LocalDate

import cats._

object LocalDateTypeClassInstances {
  implicit val localDateOrderInstance: Order[LocalDate] = Order.from[LocalDate](_ compareTo _)
}
