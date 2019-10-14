package com.timesheet.util

import java.time.Instant

import cats._
import com.avsystem.commons.serialization.GenCodec

object InstantTypeClassInstances {
  implicit val instantOrderInstance: Order[Instant] = Order.from[Instant](_ compareTo _)

  object InstantApplyUnapplyCompanion {
    def apply(value: Long): Instant             = Instant.ofEpochMilli(value)
    def unapply(instant: Instant): Option[Long] = Option(instant).map(_.toEpochMilli)
  }

  implicit val Codec: GenCodec[Instant] = GenCodec.createSimple({ input =>
    Instant.ofEpochMilli(input.readLong())
  }, { (output, instant) =>
    output.writeLong(instant.toEpochMilli)
  }, allowNull = false)
}
