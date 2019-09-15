package com.timesheet.core.store.worksamples

import cats.data._
import com.timesheet.model.db.ID
import com.timesheet.model.user.User.UserId
import com.timesheet.model.worksample.WorkSample

trait WorkSamplesStoreAlgebra[F[_]] {
  def create(workSample: WorkSample): F[WorkSample]

  def update(workSample: WorkSample): OptionT[F, WorkSample]

  def get(id: ID): OptionT[F, WorkSample]

  def getAll(): F[Seq[WorkSample]]

  def getAllForUser(userId: UserId): F[Seq[WorkSample]]

  def delete(id: ID): OptionT[F, WorkSample]

}
