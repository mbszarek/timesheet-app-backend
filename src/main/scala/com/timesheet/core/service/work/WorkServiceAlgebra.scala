package com.timesheet.core.service.work

import java.time.LocalDate

import cats.data._
import com.timesheet.core.validation.ValidationUtils.{DateValidationError, WorkSampleValidationError}
import com.timesheet.model.user.{User, UserId}
import com.timesheet.model.work.{WorkInterval, WorkSample}

import scala.concurrent.duration.FiniteDuration

trait WorkServiceAlgebra[F[_]] {
  def tagWorkerEntrance(user: User): EitherT[F, WorkSampleValidationError, WorkSample]

  def tagWorkerExit(user: User): EitherT[F, WorkSampleValidationError, WorkSample]

  def collectWorkTimeForUserBetweenDates(user: User, from: LocalDate, to: LocalDate): F[FiniteDuration]

  def collectObligatoryWorkTimeForUser(user: User, from: LocalDate, to: LocalDate): F[FiniteDuration]

  def getAllWorkSamplesBetweenDates(userId: UserId, from: LocalDate, to: LocalDate): F[List[WorkSample]]

  def getAllWorkIntervalsBetweenDates(
    user: User,
    from: LocalDate,
    to: LocalDate
  ): EitherT[F, DateValidationError, List[WorkInterval]]
}
