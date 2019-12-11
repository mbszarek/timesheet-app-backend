package com.timesheet.util

import java.time.LocalDate

import cats._
import cats.implicits._
import cats.effect._
import monix.eval.Task
import org.scalatest.flatspec.AnyFlatSpec
import monix.execution.Scheduler.Implicits.global
import LocalDateTypeClassInstances._

class DateRangeGeneratorTest extends AnyFlatSpec {
  behavior of "DateRangeGenerator"

  it should "return list containing one element for one day interval" in {
    val date      = LocalDate.now()
    val dateRange = DateRangeGenerator[Task].getDateRange(date, date).runSyncUnsafe()
    assert(dateRange.size == 1)
    assert(dateRange.contains(date))
  }

  it should "return empty list for fromDate after toDate" in {
    val date      = LocalDate.now()
    val dateRange = DateRangeGenerator[Task].getDateRange(date, date.minusDays(1L)).runSyncUnsafe()
    assert(dateRange.isEmpty)
  }

  it should "return list with ordered dates" in {
    val date      = LocalDate.now()
    val dateRange = DateRangeGenerator[Task].getDateRange(date, date.plusDays(7L)).runSyncUnsafe()
    val (isOrdered, _) = dateRange.foldLeft((true, date.minusDays(1L))) {
      case ((false, oldDate), _)   => (false, oldDate)
      case ((true, oldDate), date) => (oldDate < date, date)
    }
    assert(isOrdered)
  }

}
