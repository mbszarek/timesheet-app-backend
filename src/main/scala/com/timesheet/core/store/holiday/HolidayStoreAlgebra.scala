package com.timesheet.core.store.holiday

import java.time.LocalDate

import cats.data._
import com.avsystem.commons.serialization.GenCodec
import com.timesheet.core.store.base.StoreAlgebra
import com.timesheet.model.holiday.Holiday
import com.timesheet.model.user.UserId

import scala.reflect.ClassTag

trait HolidayStoreAlgebra[F[_]] extends StoreAlgebra[F] {
  override type K = Holiday

  protected def tag: ClassTag[Holiday]   = implicitly
  protected def codec: GenCodec[Holiday] = implicitly

  def getAllForUser(userId: UserId): F[List[Holiday]]

  def getAllForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[List[Holiday]]

  def deleteUserHolidayForDate(
    userId: UserId,
    date: LocalDate,
  ): OptionT[F, Holiday]

  def countForUserForDateRange(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[Long]
}
