package com.timesheet.core.service.work

import java.time.{LocalDate, LocalDateTime}

import cats.data._
import com.timesheet.core.service.base.EntityServiceAlgebra
import com.timesheet.core.validation.ValidationUtils.{DateValidationError, WorkSampleValidationError}
import com.timesheet.model.user.{User, UserId}
import com.timesheet.model.work.{WorkInterval, WorkSample, WorkTime}

trait WorkServiceAlgebra[F[_]] extends EntityServiceAlgebra[F] {
  override type Entity = WorkSample

  def tagWorkerEntrance(user: User): EitherT[F, WorkSampleValidationError, WorkSample]

  def tagWorkerExit(user: User): EitherT[F, WorkSampleValidationError, WorkSample]

  def collectWorkTimeForUserBetweenDates(
    user: User,
    from: LocalDate,
    to: LocalDate,
  ): EitherT[F, DateValidationError, WorkTime]

  def collectObligatoryWorkTimeForUser(
    user: User,
    from: LocalDate,
    to: LocalDate,
  ): EitherT[F, DateValidationError, WorkTime]

  def getAllWorkSamplesBetweenDates(
    userId: UserId,
    from: LocalDate,
    to: LocalDate,
  ): EitherT[F, DateValidationError, List[WorkSample]]

  def getAllWorkIntervalsBetweenDates(
    user: User,
    from: LocalDate,
    to: LocalDate,
  ): EitherT[F, DateValidationError, List[WorkInterval]]

  def wasUserAtWork(
    user: User,
    date: LocalDateTime,
  ): EitherT[F, DateValidationError, Boolean]
}
