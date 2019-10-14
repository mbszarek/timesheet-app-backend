package com.timesheet.core.service.work

import java.time.LocalDate

import cats.data._
import com.timesheet.core.validation.ValidationUtils.WorkSampleValidationError
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId
import com.timesheet.model.worksample.WorkSample

import scala.concurrent.duration.FiniteDuration

trait WorkServiceAlgebra[F[_]] {
  def tagWorkerEntrance(user: User): EitherT[F, WorkSampleValidationError, WorkSample]

  def tagWorkerExit(user: User): EitherT[F, WorkSampleValidationError, WorkSample]

  def collectWorkTimeForUserBetweenDates(userId: UserId, from: LocalDate, to: LocalDate): F[FiniteDuration]
}
