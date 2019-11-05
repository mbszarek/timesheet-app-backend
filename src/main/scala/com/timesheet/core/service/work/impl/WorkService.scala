package com.timesheet.core.service.work
package impl

import java.time.{Instant, LocalDate, LocalDateTime, ZonedDateTime}

import cats.data._
import cats.effect._
import cats.implicits._
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.core.store.worksample.WorkSampleStoreAlgebra
import com.timesheet.core.validation.ValidationUtils
import com.timesheet.core.validation.ValidationUtils.{DateValidationError, WorkSampleValidationError, WrongUserState}
import com.timesheet.core.validation.date.DateValidatorAlgebra
import com.timesheet.core.validation.worksample.WorkSampleValidatorAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.user.{User, UserId}
import com.timesheet.model.work.ActivityType.EqInstance
import com.timesheet.model.work._
import com.timesheet.util.DateRangeGenerator
import com.timesheet.util.InstantTypeClassInstances.instantOrderInstance
import com.timesheet.util.LocalDateTimeTypeClassInstances.localDateTimeOrderInstance

import scala.annotation.tailrec

final class WorkService[F[_]: Sync](
  userStore: UserStoreAlgebra[F],
  workSampleStore: WorkSampleStoreAlgebra[F],
  workSampleValidator: WorkSampleValidatorAlgebra[F],
  dateValidatorAlgebra: DateValidatorAlgebra[F],
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

  def collectWorkTimeForUserBetweenDates(user: User, from: LocalDate, to: LocalDate): F[WorkTime] = {
    val fromDate = from.atStartOfDay()
    val toDate   = to.plusDays(1).atStartOfDay()

    for {
      workSamples <- workSampleStore.getAllForUserBetweenDates(user.id, fromDate, toDate)
      dateRangeGenerator = DateRangeGenerator[F]
      dateRange <- dateRangeGenerator.getDateRange(from, to)
      groupedWorkSamples = workSamples.groupBy(_.date.toLocalDate())
      newWorkSamples     = dateRange.map(date => date -> groupedWorkSamples.getOrElse(date, List.empty).reverse).reverse
      result <- Sync[F].delay(countTime(newWorkSamples, user.isCurrentlyAtWork))
    } yield result
  }

  def collectObligatoryWorkTimeForUser(user: User, from: LocalDate, to: LocalDate): F[WorkTime] =
    for {
      dateRangeGenerator <- DateRangeGenerator[F].pure[F]
      dateRange          <- dateRangeGenerator.getDateRange(from, to)
    } yield {
      dateRange.foldLeft(WorkTime.empty) {
        case (duration, localDate) =>
          val workingHours = localDate.getDayOfWeek match {
            //          case DayOfWeek.SATURDAY =>
            //            0.hour
            //          case DayOfWeek.SUNDAY =>
            //            0.hour
            case _ =>
              WorkTime.fromMillis((user.workingHours * 60 * 60 * 1000 / 5).toLong)
          }
          duration |+| workingHours
      }
    }

  def getAllWorkSamplesBetweenDates(userId: UserId, from: LocalDate, to: LocalDate): F[List[WorkSample]] =
    workSampleStore.getAllForUserBetweenDates(userId, from.atStartOfDay(), to.plusDays(1).atStartOfDay())

  def getAllWorkIntervalsBetweenDates(
    user: User,
    from: LocalDate,
    to: LocalDate
  ): EitherT[F, DateValidationError, List[WorkInterval]] =
    for {
      _ <- dateValidatorAlgebra.isDateInTheFuture(to)
      result <- EitherT.liftF {
        for {
          workSamples <- getAllWorkSamplesBetweenDates(user.id, from, to.plusDays(1L))
          dateRangeGenerator = DateRangeGenerator[F]
          dateRange <- dateRangeGenerator.getDateRange(from, to)
          groupedWorkSamples = workSamples.groupBy(_.date.toLocalDate())
          newWorkSamples     = dateRange.map(date => date -> groupedWorkSamples.getOrElse(date, List.empty).reverse).reverse
          result             = collectAllWorkIntervals(newWorkSamples, user.isCurrentlyAtWork)
        } yield result
      }
    } yield result

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
  private def countTime(
    list: List[(LocalDate, List[WorkSample])],
    wasAtWork: Boolean = false,
    totalTime: WorkTime = WorkTime.empty,
  ): WorkTime = list match {
    case Nil =>
      totalTime
    case (localDate, workSamples) :: tail =>
      val (dayTime, newWasAtWork) =
        if (workSamples.isEmpty && wasAtWork)
          (
            WorkTime.fromMillis(
              getNextAtStartOfDayInstant(localDate).toEpochMilli - localDate.toInstant().toEpochMilli
            ),
            wasAtWork,
          )
        else
          countDayTime(workSamples, totalTime)
      countTime(tail, newWasAtWork, totalTime |+| dayTime)
  }

  @tailrec
  private def countDayTime(
    list: List[WorkSample],
    totalTime: WorkTime = WorkTime.empty,
    wasAtWork: Boolean = false,
  ): (WorkTime, Boolean) = list match {
    case Nil =>
      (totalTime, wasAtWork)
    case first :: second :: tail if first.activityType === Departure && second.activityType === Entrance =>
      countDayTime(tail, totalTime |+| WorkTime.fromMillis(first.date.toEpochMilli - second.date.toEpochMilli))
    case sample :: tail =>
      val date = sample.date.toLocalDate()
      val (newTotalTime, wasAtWork) = sample.activityType match {
        case Departure =>
          (WorkTime.fromMillis(sample.date.toEpochMilli - date.toInstant().toEpochMilli), true)
        case Entrance =>
          (WorkTime.fromMillis(getNextAtStartOfDayInstant(date).toEpochMilli - sample.date.toEpochMilli), false)
      }
      countDayTime(tail, totalTime |+| newTotalTime, wasAtWork)
  }

  @tailrec
  private def collectAllWorkIntervals(
    list: List[(LocalDate, List[WorkSample])],
    wasAtWork: Boolean = false,
    result: List[WorkInterval] = List.empty,
  ): List[WorkInterval] = list match {
    case Nil =>
      result
    case (localDate, workSamples) :: tail =>
      val (workIntervals, newWasAtWork): (List[WorkInterval], Boolean) =
        collectDayWorkIntervals(workSamples, wasAtWork, localDate.plusDays(1L).atStartOfDay().min(LocalDateTime.now()))
      collectAllWorkIntervals(tail, newWasAtWork, result ++ workIntervals)
  }

  @tailrec
  private def collectDayWorkIntervals(
    list: List[WorkSample],
    wasAtWork: Boolean,
    lastDateTime: LocalDateTime,
    result: List[WorkInterval] = List.empty,
  ): (List[WorkInterval], Boolean) = list match {
    case Nil =>
      val fromDate = lastDateTime.toLocalDate.atStartOfDay()
      val workInterval = WorkInterval(
        if (fromDate === lastDateTime) lastDateTime.toLocalDate.minusDays(1L).atStartOfDay() else fromDate,
        lastDateTime,
        wasAtWork,
      )
      (result :+ workInterval, wasAtWork)
    case sample :: tail if sample.activityType === Entrance =>
      val newLastDateTime = sample.date.toLocalDateTime()
      val workInterval = WorkInterval(
        newLastDateTime,
        lastDateTime,
        wasAtWork = true,
      )
      collectDayWorkIntervals(tail, wasAtWork = false, newLastDateTime, result :+ workInterval)
    case sample :: tail if sample.activityType === Departure =>
      val newLastDateTime = sample.date.toLocalDateTime()
      val workInterval = WorkInterval(
        newLastDateTime,
        lastDateTime,
        wasAtWork = false,
      )
      collectDayWorkIntervals(tail, wasAtWork = true, newLastDateTime, result :+ workInterval)
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
    workSampleValidator: WorkSampleValidatorAlgebra[F],
    dateValidatorAlgebra: DateValidatorAlgebra[F],
  ): WorkService[F] =
    new WorkService[F](userStore, workSampleStore, workSampleValidator, dateValidatorAlgebra)
}
