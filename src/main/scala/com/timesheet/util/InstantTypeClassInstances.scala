package com.timesheet.util

import java.time.Instant

import cats._
import com.avsystem.commons.serialization.GenCodec

object InstantTypeClassInstances {
  implicit val Codec: GenCodec[Instant] = GenCodec.nonNullSimple(
    input => Instant.ofEpochMilli(input.readTimestamp()),
    (output, value) => output.writeTimestamp(value.toEpochMilli),
  )

  implicit val instantOrderInstance: Order[Instant] = Order.fromComparable[Instant]
}
