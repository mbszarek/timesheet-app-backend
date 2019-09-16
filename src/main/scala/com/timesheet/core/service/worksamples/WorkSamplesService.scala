package com.timesheet.core.service.worksamples

import java.time.Instant

import cats._
import cats.data._
import cats.implicits._
import com.timesheet.core.store.worksamples.WorkSamplesStoreAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.user.User.UserId
import com.timesheet.model.worksample.{ActivityType, Departure, Entrance, WorkSample}

class WorkSamplesService[F[_]](workSamplesStore: WorkSamplesStoreAlgebra[F]) {
  def tagWorkerEntrance(userId: UserId): F[WorkSample] =
    workSamplesStore.create(
      createWorkSample(userId, Entrance)
    )

  def tagWorkerExit(userId: UserId): F[WorkSample] =
    workSamplesStore.create(
      createWorkSample(userId, Departure)
    )

  private def createWorkSample(userId: UserId, activityType: ActivityType): WorkSample = WorkSample(
    ID.createNew(),
    userId,
    activityType,
    Instant.now(),
  )
}

object WorkSamplesService {
  def apply[F[_]](workSamplesStore: WorkSamplesStoreAlgebra[F]): WorkSamplesService[F] =
    new WorkSamplesService[F](workSamplesStore)
}
