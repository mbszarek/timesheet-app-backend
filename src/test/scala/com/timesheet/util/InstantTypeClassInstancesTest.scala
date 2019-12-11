package com.timesheet
package util

import cats._
import java.time.{Instant, LocalDate}
import org.scalatest.flatspec.AnyFlatSpec

class InstantTypeClassInstancesTest extends AnyFlatSpec {
  behavior of "Instant Order type class instance"

  it should "return true for instant after instant happened before" in {
    import InstantTypeClassInstances.instantOrderInstance
    import cats.syntax.order._

    val date = LocalDate.now()
    val olderInstant = date.toInstant()
    val newerInstant = date.plusDays(1L).toInstant()
    assert(olderInstant < newerInstant)
  }

  it should "return true for comparing same instant" in {
    import InstantTypeClassInstances.instantOrderInstance

    val instant: Instant = LocalDate.now().toInstant()
    assert(Eq[Instant].eqv(instant, instant))
  }
}
