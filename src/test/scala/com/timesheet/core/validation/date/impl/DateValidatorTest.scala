package com.timesheet.core.validation.date.impl

import java.time.LocalDateTime

import org.scalatest.flatspec.AnyFlatSpec
import cats._
import com.timesheet.core.validation.date.DateValidatorAlgebra

class DateValidatorTest extends AnyFlatSpec {
  private val dateValidator: DateValidatorAlgebra[Id] = DateValidator[Id]

  behavior of "isDateInTheFuture"

  it should "return validation error if date is in the past" in {
    assert(dateValidator.isDateInTheFuture(LocalDateTime.now().minusDays(1L)).value.isLeft)
  }

  it should "return unit if date is in the future" in {
    assert(dateValidator.isDateInTheFuture(LocalDateTime.now().plusDays(1L)).value.isRight)
  }

  behavior of "isDateInThePast"

  it should "return validation error if date is in the future" in {
    assert(dateValidator.isDateInThePast(LocalDateTime.now().plusDays(1L)).value.isLeft)
  }

  it should "return unit if date is in the past" in {
    assert(dateValidator.isDateInThePast(LocalDateTime.now().minusDays(1L)).value.isRight)
  }

  behavior of "areDatesInProperOrder"

  it should "return unit for ascending dates" in {
    val firstDate  = LocalDateTime.now()
    val secondDate = firstDate.plusDays(1L)
    assert(dateValidator.areDatesInProperOrder(firstDate, secondDate).value.isRight)
  }

  it should "return unit for the same date" in {
    val date  = LocalDateTime.now()
    assert(dateValidator.areDatesInProperOrder(date, date).value.isRight)
  }

  it should "return validation error for ascending dates" in {
    val firstDate  = LocalDateTime.now()
    val secondDate = firstDate.minusDays(1L)
    assert(dateValidator.areDatesInProperOrder(firstDate, secondDate).value.isLeft)
  }
}
