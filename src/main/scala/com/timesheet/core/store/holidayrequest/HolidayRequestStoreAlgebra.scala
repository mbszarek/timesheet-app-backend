package com.timesheet.core.store.holidayrequest

import java.time.LocalDate

import cats.data.OptionT
import com.avsystem.commons.serialization.GenCodec
import com.timesheet.core.store.base.StoreAlgebra
import com.timesheet.model.holidayrequest.HolidayRequest
import com.timesheet.model.user.UserId

import scala.reflect.ClassTag

trait HolidayRequestStoreAlgebra[F[_]] extends StoreAlgebra[F] {

  override type K = HolidayRequest

  protected def tag: ClassTag[HolidayRequest]   = implicitly
  protected def codec: GenCodec[HolidayRequest] = implicitly

  def getAllForUser(userId: UserId): F[List[HolidayRequest]]

  def getAllForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[List[HolidayRequest]]

  def getAllBetweenDates(
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[List[HolidayRequest]]

  def deleteUserHolidayRequestForDate(
    userId: UserId,
    date: LocalDate,
  ): OptionT[F, HolidayRequest]

  def countForUserForDateRange(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[Long]
}
