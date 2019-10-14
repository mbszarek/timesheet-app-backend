package com.timesheet.core.service.work.impl

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime}

import cats.data._
import cats.effect._
import cats.implicits._
import com.timesheet.concurrent.FutureConcurrentEffect
import com.timesheet.core.service.work.WorkServiceAlgebra
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.core.store.worksample.WorkSampleStoreAlgebra
import com.timesheet.core.validation.ValidationUtils
import com.timesheet.core.validation.ValidationUtils.{WorkSampleValidationError, WrongUserState}
import com.timesheet.core.validation.worksample.WorkSampleValidatorAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId
import com.timesheet.model.worksample.{ActivityType, Departure, Entrance, WorkSample}

import scala.annotation.tailrec
import scala.concurrent.duration._

class WorkService[F[_]: FutureConcurrentEffect](
  userStore: UserStoreAlgebra[F],
  workSampleStore: WorkSampleStoreAlgebra[F],
  workSampleValidator: WorkSampleValidatorAlgebra[F],
) extends WorkServiceAlgebra[F] {
  def tagWorkerEntrance(user: User): EitherT[F, WorkSampleValidationError, WorkSample] =
    for {
      _          <- workSampleValidator.hasUserCorrectState(user, Entrance)
      _          <- changeUserStatus(user, defaultValue = true)
      workSample <- EitherT.liftF(workSampleStore.create(createWorkSample(user.id, Entrance)))
    } yield workSample

  def tagWorkerExit(user: User): EitherT[F, WorkSampleValidationError, WorkSample] =
    for {
      _          <- workSampleValidator.hasUserCorrectState(user, Departure)
      _          <- changeUserStatus(user, defaultValue = false)
      workSample <- EitherT.liftF(workSampleStore.create(createWorkSample(user.id, Departure)))
    } yield workSample

  def collectWorkTimeForUserBetweenDates(userId: UserId, from: LocalDate, to: LocalDate): F[FiniteDuration] = {
    val fromDate = from.atStartOfDay()
    val toDate   = to.plusDays(1).atStartOfDay()

    for {
      workSamples <- workSampleStore.getAllForUserBetweenDates(userId, fromDate, toDate)
      result      <- Sync[F].delay(countTime(workSamples))
    } yield result
  }

  private def createWorkSample(userId: UserId, activityType: ActivityType): WorkSample = WorkSample(
    ID.createNew(),
    userId,
    activityType,
    Instant.now(),
  )

  private def changeUserStatus(
    user: User,
    defaultValue: Boolean,
  ): EitherT[F, ValidationUtils.WrongUserState.type, User] =
    EitherT.fromOptionF(
      userStore.update(user.copy(isCurrentlyAtWork = user.isCurrentlyAtWork.map(!_).orElse(defaultValue.some))).value,
      WrongUserState,
    )

  @tailrec
  private def countTime(list: List[WorkSample], totalTime: FiniteDuration = 0.second): FiniteDuration = list match {
    case Nil =>
      totalTime
    case first :: second :: tail if first.activityType == Entrance && second.activityType == Departure =>
      countTime(tail, totalTime + (second.date.toEpochMilli - first.date.toEpochMilli).milli)
    case sample :: tail =>
      val date = sample.date.atZone(ZoneId.systemDefault()).toLocalDate
      val newTotalTime = totalTime + (sample.activityType match {
        case Departure =>
          sample.date.toEpochMilli - date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli
        case Entrance =>
          import com.timesheet.util.InstantTypeClassInstances.instantOrderInstance

          date
            .plusDays(1)
            .atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant
            .min {
              ZonedDateTime.now().toInstant
            }
            .toEpochMilli - sample.date.toEpochMilli
      }).milli
      countTime(tail, newTotalTime)
  }
}

object WorkService {
  def apply[F[_]: FutureConcurrentEffect](
    userStore: UserStoreAlgebra[F],
    workSampleStore: WorkSampleStoreAlgebra[F],
    workSampleValidator: WorkSampleValidatorAlgebra[F]
  ): WorkService[F] =
    new WorkService[F](userStore, workSampleStore, workSampleValidator)
}
