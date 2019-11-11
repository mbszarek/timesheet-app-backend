package com.timesheet.core.service.work

import java.time.{LocalDate, LocalDateTime}

import cats.data._
import com.timesheet.core.validation.ValidationUtils.{DateValidationError, WorkSampleValidationError}
import com.timesheet.model.user.{User, UserId}
import com.timesheet.model.work.{WorkInterval, WorkSample, WorkTime}

trait WorkServiceAlgebra[F[_]] {
  def tagWorkerEntrance(user: User): EitherT[F, WorkSampleValidationError, WorkSample]

  def tagWorkerExit(user: User): EitherT[F, WorkSampleValidationError, WorkSample]

  def collectWorkTimeForUserBetweenDates(
    user: User,
    from: LocalDate,
    to: LocalDate,
  ): F[WorkTime]

  def collectObligatoryWorkTimeForUser(
    user: User,
    from: LocalDate,
    to: LocalDate,
  ): F[WorkTime]

  def getAllWorkSamplesBetweenDates(
    userId: UserId,
    from: LocalDate,
    to: LocalDate,
  ): F[List[WorkSample]]

  def getAllWorkIntervalsBetweenDates(
    user: User,
    from: LocalDate,
    to: LocalDate,
  ): EitherT[F, DateValidationError, List[WorkInterval]]

  def wasUserAtWork(
    user: User,
    date: LocalDateTime,
  ): F[Boolean]
}
