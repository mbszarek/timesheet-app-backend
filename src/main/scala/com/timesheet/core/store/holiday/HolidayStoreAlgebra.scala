package com.timesheet.core.store.holiday

import java.time.LocalDate

import cats.data._
import com.timesheet.model.db.ID
import com.timesheet.model.holiday.Holiday
import com.timesheet.model.user.UserId

trait HolidayStoreAlgebra[F[_]] {
  def create(holiday: Holiday): F[Holiday]

  def update(holiday: Holiday): OptionT[F, Holiday]

  def get(id: ID): OptionT[F, Holiday]

  def getAll(): F[List[Holiday]]

  def getAllForUser(userId: UserId): F[List[Holiday]]

  def getAllForUserBetweenDates(userId: UserId, fromDate: LocalDate, toDate: LocalDate): F[List[Holiday]]

  def delete(id: ID): OptionT[F, Holiday]

  def deleteUserHolidayForDate(
    userId: UserId,
    date: LocalDate,
  ): OptionT[F, Holiday]
}
