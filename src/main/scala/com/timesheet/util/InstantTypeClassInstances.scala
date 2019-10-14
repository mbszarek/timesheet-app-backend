package com.timesheet.util

import java.time.Instant

import cats._

object InstantTypeClassInstances {
  implicit val instantOrderInstance: Order[Instant] = Order.from[Instant](_ compareTo _)
}
