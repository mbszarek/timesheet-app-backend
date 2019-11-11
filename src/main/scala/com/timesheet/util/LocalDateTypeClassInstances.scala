package com.timesheet
package util

import java.time.{Instant, LocalDate}

import cats._
import com.avsystem.commons.serialization.GenCodec

object LocalDateTypeClassInstances {
  implicit val localDateOrderInstance: Order[LocalDate] = Order.from[LocalDate](_ compareTo _)

  implicit val Codec: GenCodec[LocalDate] = GenCodec.nonNullSimple({ input =>
    Instant
      .ofEpochMilli(input.readLong())
      .toLocalDate()
  }, { (output, date) =>
    output
      .writeLong(date.toInstant().toEpochMilli)
  })
}
