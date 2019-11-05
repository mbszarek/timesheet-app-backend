package com.timesheet.core.store.worksample

import java.time.LocalDateTime

import cats.data._
import com.timesheet.model.db.ID
import com.timesheet.model.user.UserId
import com.timesheet.model.work.WorkSample

trait WorkSampleStoreAlgebra[F[_]] {
  def create(workSample: WorkSample): F[WorkSample]

  def update(workSample: WorkSample): OptionT[F, WorkSample]

  def get(id: ID): OptionT[F, WorkSample]

  def getAll(): F[List[WorkSample]]

  def getAllForUserBetweenDates(userId: UserId, from: LocalDateTime, to: LocalDateTime): F[List[WorkSample]]

  def wasAtWork(userId: UserId, date: LocalDateTime): F[Boolean]

  def getAllForUser(userId: UserId): F[List[WorkSample]]

  def delete(id: ID): OptionT[F, WorkSample]

}
