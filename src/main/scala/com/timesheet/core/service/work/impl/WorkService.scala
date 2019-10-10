package com.timesheet.core.service.work.impl

import java.time.LocalDate

import cats.data.OptionT
import com.timesheet.concurrent.FutureConcurrentEffect
import com.timesheet.core.service.work.WorkServiceAlgebra
import com.timesheet.core.store.worksample.WorkSampleStoreAlgebra
import com.timesheet.model.user.User

import scala.concurrent.duration.FiniteDuration

class WorkService[F[_]: FutureConcurrentEffect](workSampleStore: WorkSampleStoreAlgebra[F])
    extends WorkServiceAlgebra[F] {
  override def collectDayWorkTimeForUser(userId: User.UserId, day: LocalDate): OptionT[F, FiniteDuration] = {

    ???
  }
}
