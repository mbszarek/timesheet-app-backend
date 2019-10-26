package com.timesheet.core.service.work
package impl

import java.time.{Instant, LocalDate, ZoneOffset, ZonedDateTime}

import cats.data._
import cats.effect._
import cats.implicits._
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.core.store.worksample.WorkSampleStoreAlgebra
import com.timesheet.core.validation.ValidationUtils
import com.timesheet.core.validation.ValidationUtils.{WorkSampleValidationError, WrongUserState}
import com.timesheet.core.validation.worksample.WorkSampleValidatorAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.user.{User, UserId}
import com.timesheet.model.worksample.ActivityType.EqInstance
import com.timesheet.model.worksample.{ActivityType, Departure, Entrance, WorkSample}
import com.timesheet.util.DateRangeGenerator
import com.timesheet.util.InstantTypeClassInstances.instantOrderInstance
import com.timesheet.util.LocalDateTypeClassInstances.localDateOrderInstance

import scala.annotation.tailrec
import scala.concurrent.duration._

class WorkService[F[_]: Sync](
  userStore: UserStoreAlgebra[F],
  workSampleStore: WorkSampleStoreAlgebra[F],
  workSampleValidator: WorkSampleValidatorAlgebra[F],
) extends WorkServiceAlgebra[F] {
  def tagWorkerEntrance(user: User): EitherT[F, WorkSampleValidationError, WorkSample] =
    for {
      _          <- workSampleValidator.hasUserCorrectState(user, Entrance)
      _          <- changeUserStatus(user)
      workSample <- EitherT.liftF(workSampleStore.create(createWorkSample(user.id, Entrance)))
    } yield workSample

  def tagWorkerExit(user: User): EitherT[F, WorkSampleValidationError, WorkSample] =
    for {
      _          <- workSampleValidator.hasUserCorrectState(user, Departure)
      _          <- changeUserStatus(user)
      workSample <- EitherT.liftF(workSampleStore.create(createWorkSample(user.id, Departure)))
    } yield workSample

  def collectWorkTimeForUserBetweenDates(user: User, from: LocalDate, to: LocalDate): F[FiniteDuration] = {
    val fromDate = from.atStartOfDay()
    val toDate   = to.plusDays(1).atStartOfDay()

    for {
      workSamples <- workSampleStore.getAllForUserBetweenDates(user.id, fromDate, toDate)
      dateRangeGenerator = DateRangeGenerator[F]
      dateRange <- dateRangeGenerator.getDateRange(from, to)
      groupedWorkSamples = workSamples.groupBy(_.date.toLocalDate())
      newWorkSamples     = dateRange.map(date => date -> groupedWorkSamples.getOrElse(date, List.empty).reverse).reverse
      result <- Sync[F].delay(newCountTime(newWorkSamples, user.isCurrentlyAtWork))
    } yield result
  }

  def collectObligatoryWorkTimeForUser(user: User, from: LocalDate, to: LocalDate): F[FiniteDuration] = {
    @tailrec
    def countTime(date: LocalDate, totalTime: FiniteDuration = 0.hour): FiniteDuration =
      if (date === to)
        totalTime
      else {
        val workingHours = date.getDayOfWeek match {
          //          case DayOfWeek.SATURDAY =>
          //            0.hour
          //          case DayOfWeek.SUNDAY =>
          //            0.hour
          case _ =>
            (user.workingHours / 5).hour
        }
        countTime(date.plusDays(1L), totalTime + workingHours)
      }

    Sync[F].delay(countTime(from))
  }

  def getAllWorkSamplesBetweenDates(userId: UserId, from: LocalDate, to: LocalDate): F[List[WorkSample]] =
    workSampleStore.getAllForUserBetweenDates(userId, from.atStartOfDay(), to.plusDays(1).atStartOfDay())

  private def createWorkSample(userId: UserId, activityType: ActivityType): WorkSample = WorkSample(
    ID.createNew(),
    userId,
    activityType,
    Instant.now(),
  )

  private def changeUserStatus(
    user: User,
  ): EitherT[F, ValidationUtils.WrongUserState.type, User] =
    EitherT.fromOptionF(
      userStore.update(user.copy(isCurrentlyAtWork = !user.isCurrentlyAtWork)).value,
      WrongUserState,
    )

  @tailrec
  private def countDayTime(
    list: List[WorkSample],
    totalTime: FiniteDuration = 0.second,
    wasAtWork: Boolean = false,
  ): (FiniteDuration, Boolean) = list match {
    case Nil =>
      (totalTime, wasAtWork)
    case first :: second :: tail if first.activityType === Departure && second.activityType === Entrance =>
      countDayTime(tail, totalTime + (first.date.toEpochMilli - second.date.toEpochMilli).milli)
    case sample :: tail =>
      val date = sample.date.toLocalDate()
      val (newTotalTime, wasAtWork) = sample.activityType match {
        case Departure =>
          ((sample.date.toEpochMilli - date.toInstant().toEpochMilli).milli, true)
        case Entrance =>
          ((getNextAtStartOfDayInstant(date).toEpochMilli - sample.date.toEpochMilli).milli, false)
      }
      countDayTime(tail, totalTime + newTotalTime, wasAtWork)
  }

  @tailrec
  private def newCountTime(
    list: List[(LocalDate, List[WorkSample])],
    wasAtWork: Boolean = false,
    totalTime: FiniteDuration = 0.second,
  ): FiniteDuration = list match {
    case Nil =>
      totalTime
    case (localDate, workSamples) :: tail =>
      val (dayTime, newWasAtWork) =
        if (workSamples.isEmpty && wasAtWork)
          ((getNextAtStartOfDayInstant(localDate).toEpochMilli - localDate.toInstant().toEpochMilli).milli, wasAtWork)
        else
          countDayTime(workSamples, totalTime)
      newCountTime(tail, newWasAtWork, totalTime + dayTime)
  }

  private def getNextAtStartOfDayInstant(date: LocalDate): Instant =
    date
      .plusDays(1)
      .toInstant()
      .min {
        ZonedDateTime.now().toInstant
      }

}

object WorkService {
  def apply[F[_]: Sync](
    userStore: UserStoreAlgebra[F],
    workSampleStore: WorkSampleStoreAlgebra[F],
    workSampleValidator: WorkSampleValidatorAlgebra[F]
  ): WorkService[F] =
    new WorkService[F](userStore, workSampleStore, workSampleValidator)
}
