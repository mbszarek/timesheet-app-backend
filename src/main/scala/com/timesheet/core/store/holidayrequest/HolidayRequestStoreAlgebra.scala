package com.timesheet.core.store.holidayrequest

import java.time.LocalDate

import cats.data.OptionT
import com.avsystem.commons.mongo.BsonRef
import com.avsystem.commons.serialization.GenCodec
import com.timesheet.core.store.base.StoreAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.holidayrequest.HolidayRequest
import com.timesheet.model.user.UserId

import scala.reflect.ClassTag

trait HolidayRequestStoreAlgebra[F[_]] extends StoreAlgebra[F] {

  override type K = HolidayRequest

  protected def tag: ClassTag[HolidayRequest]      = implicitly
  protected def codec: GenCodec[HolidayRequest]    = implicitly
  protected def idRef: BsonRef[HolidayRequest, ID] = implicitly

  def getAllPendingForUser(userId: UserId): F[List[HolidayRequest]]

  def getAllPendingForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[List[HolidayRequest]]

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

  def countPendingForUserForDateRange(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[Long]
}
