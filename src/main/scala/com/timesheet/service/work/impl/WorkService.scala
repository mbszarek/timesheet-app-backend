package com.timesheet
package service.work.impl

import java.time._

import cats.data._
import cats.effect._
import cats.implicits._
import com.timesheet.core.error.ValidationErrors.{DateValidationError, WorkSampleValidationError, WrongUserState}
import com.timesheet.service.base.EntityServiceAlgebra.EntityStore
import com.timesheet.service.work.WorkServiceAlgebra
import com.timesheet.core.store.holiday.HolidayStoreAlgebra
import com.timesheet.core.store.user.UserStoreAlgebra
import com.timesheet.core.store.worksample.WorkSampleStoreAlgebra
import com.timesheet.core.validation.date.DateValidatorAlgebra
import com.timesheet.core.validation.worksample.WorkSampleValidatorAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.holiday.Holiday
import com.timesheet.model.user.{User, UserId}
import com.timesheet.model.work.ActivityType.EqInstance
import com.timesheet.model.work._
import com.timesheet.service.impl
import com.timesheet.util.DateRangeGenerator
import com.timesheet.util.LocalDateTimeTypeClassInstances.localDateTimeOrderInstance
import com.timesheet.util.LocalDateTypeClassInstances.localDateOrderInstance
import com.timesheet.util.InstantTypeClassInstances.instantOrderInstance
import scala.annotation.tailrec
import scala.concurrent.duration._

final class WorkService[F[_]: Sync](
  userStore: UserStoreAlgebra[F],
  workSampleStore: WorkSampleStoreAlgebra[F],
  workSampleValidator: WorkSampleValidatorAlgebra[F],
  holidayStore: HolidayStoreAlgebra[F],
  dateValidator: DateValidatorAlgebra[F],
) extends impl.EntityServiceImpl[F]
    with WorkServiceAlgebra[F] {

  override protected def entityStore: EntityStore[F, WorkSample] = workSampleStore

  def tagWorkerEntrance(user: User): EitherT[F, WorkSampleValidationError, WorkSample] =
    for {
      _ <- workSampleValidator
        .hasUserCorrectState(user, Entrance)

      _ <- changeUserStatus(user)

      workSample <- EitherT
        .right[WorkSampleValidationError](workSampleStore.create(createWorkSample(user.id, Entrance)))
    } yield workSample

  def tagWorkerExit(user: User): EitherT[F, WorkSampleValidationError, WorkSample] =
    for {
      _ <- workSampleValidator
        .hasUserCorrectState(user, Departure)

      _ <- changeUserStatus(user)

      workSample <- EitherT
        .right[WorkSampleValidationError](workSampleStore.create(createWorkSample(user.id, Departure)))
    } yield workSample

  def collectWorkTimeForUserBetweenDates(
    user: User,
    from: LocalDate,
    to: LocalDate,
  ): EitherT[F, DateValidationError, WorkTime] = {
    val fromDate = from.atStartOfDay()
    val toDate   = to.plusDays(1).atStartOfDay()

    for {
      _ <- checkDateOrder(dateValidator, from, to)

      workTime <- EitherT
        .right[DateValidationError] {
          for {
            workSamples <- workSampleStore
              .getAllForUserBetweenDates(user.id, fromDate, toDate)

            holidays <- getHolidayMap {
              holidayStore
                .getAllForUserBetweenDates(user.id, fromDate.toLocalDate, toDate.toLocalDate)
            }

            dateRange <- DateRangeGenerator[F]
              .getDateRange(from, to)

            wasAtWork <- workSampleStore
              .wasAtWork(user, toDate)

            groupedWorkSamples = workSamples
              .groupBy(_.date.toLocalDate())

            newWorkSamples = extractWorkSamples(dateRange, groupedWorkSamples, holidays)

            result <- Sync[F]
              .delay(countTime(newWorkSamples, user, wasAtWork))
          } yield result
        }
    } yield workTime
  }

  def collectObligatoryWorkTimeForUser(
    user: User,
    from: LocalDate,
    to: LocalDate,
  ): EitherT[F, DateValidationError, WorkTime] =
    for {
      _ <- checkDateOrder(dateValidator, from, to)

      dateRange <- EitherT
        .right[DateValidationError](DateRangeGenerator[F].getDateRange(from, to))
    } yield {
      dateRange.foldLeft(WorkTime.empty) {
        case (duration, localDate) =>
          val workingHours = getObligatoryWorkTimeForDay(user, localDate)
          duration |+| workingHours
      }
    }

  def getAllWorkSamplesBetweenDates(
    userId: UserId,
    from: LocalDate,
    to: LocalDate,
  ): EitherT[F, DateValidationError, List[WorkSample]] =
    for {
      _ <- checkDateOrder(dateValidator, from, to)

      workSamples <- EitherT
        .right[DateValidationError] {
          workSampleStore
            .getAllForUserBetweenDates(userId, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
        }
    } yield workSamples

  def getAllWorkIntervalsBetweenDates(
    user: User,
    from: LocalDate,
    to: LocalDate,
  ): EitherT[F, DateValidationError, List[WorkInterval]] =
    for {
      _ <- checkDateOrder(dateValidator, from, to)

      result <- EitherT.right[DateValidationError] {
        for {
          holidays <- getHolidayMap {
            holidayStore
              .getAllForUserBetweenDates(user.id, from, to.plusDays(1L))
          }

          workSamples <- workSampleStore
            .getAllForUserBetweenDates(user.id, from.atStartOfDay(), to.plusDays(1).atStartOfDay())

          wasAtWork <- checkWasAtWork(user, to)

          dateRange <- DateRangeGenerator[F]
            .getDateRange(from, to)

          groupedWorkSamples = workSamples
            .groupBy(_.date.toLocalDate())

          newWorkSamples = extractWorkSamples(dateRange, groupedWorkSamples, holidays)

          result <- Sync[F]
            .delay(collectAllWorkIntervals(newWorkSamples, wasAtWork))
        } yield result
      }
    } yield result

  def wasUserAtWork(
    user: User,
    date: LocalDateTime,
  ): EitherT[F, DateValidationError, Boolean] =
    for {
      _ <- dateValidator
        .isDateInThePast(date)

      result <- EitherT
        .right[DateValidationError](workSampleStore.wasAtWork(user, date))
    } yield result

  def getWorkTimeForUserGroupedByDate(
    user: User,
    from: LocalDate,
    to: LocalDate,
  ): EitherT[F, DateValidationError, Map[LocalDate, (WorkTime, WorkTime)]] =
    for {
      _ <- dateValidator
        .areDatesInProperOrder(from.atStartOfDay(), to.atStartOfDay())

      result <- EitherT.right[DateValidationError] {
        for {
          holidays <- getHolidayMap {
            holidayStore
              .getAllForUserBetweenDates(user.id, from, to.plusDays(1L))
          }

          workSamples <- workSampleStore
            .getAllForUserBetweenDates(user.id, from.atStartOfDay(), to.plusDays(1).atStartOfDay())

          wasAtWork <- checkWasAtWork(user, to)

          groupedWorkSamples = workSamples
            .groupBy(_.date.toLocalDate())

          dateRange <- DateRangeGenerator[F]
            .getDateRange(from, to)

          newWorkSamples = extractWorkSamples(dateRange, groupedWorkSamples, holidays)
        } yield collectGroupedWorkTime(
          newWorkSamples,
          user,
          wasAtWork,
        )
      }

      obligatoryWorkTime <- EitherT
        .right[DateValidationError](collectGroupedObligatoryWorkTimeForUser(user, from, to))

    } yield {
      (result.keySet ++ obligatoryWorkTime.keySet)
        .map { date =>
          date -> (result(date), obligatoryWorkTime(date))
        }
    }.toMap

  private def collectGroupedObligatoryWorkTimeForUser(
    user: User,
    from: LocalDate,
    to: LocalDate,
  ): F[Map[LocalDate, WorkTime]] = {
    for {
      date <- DateRangeGenerator[F]
        .getDateStream(from, to)
    } yield date -> getObligatoryWorkTimeForDay(user, date)
  }.compile.toMap

  private def getObligatoryWorkTimeForDay(
    user: User,
    date: LocalDate,
  ): WorkTime = date.getDayOfWeek match {
    case DayOfWeek.SATURDAY | DayOfWeek.SUNDAY =>
      WorkTime.empty
    case _ =>
      WorkTime.fromFiniteDuration((user.workingHours / 5).hour)
  }

  private def getHolidayMap(holidays: F[List[Holiday]]): F[Map[LocalDate, Holiday]] =
    holidays
      .flatMap { holidays =>
        holidays
          .flatTraverse { holiday =>
            DateRangeGenerator[F]
              .getDateStream(holiday.fromDate, holiday.toDate)
              .map(date => date -> holiday)
              .compile
              .toList
          }
          .map(_.toMap)
      }

  private def createWorkSample(
    userId: UserId,
    activityType: ActivityType,
  ): WorkSample = WorkSample(
    ID.createNew(),
    userId,
    activityType,
    Instant.now(),
  )

  private def changeUserStatus(user: User): EitherT[F, WrongUserState.type, User] =
    EitherT.fromOptionF(
      userStore.update(user.copy(isCurrentlyAtWork = !user.isCurrentlyAtWork)).value,
      WrongUserState,
    )

  private def checkWasAtWork(
    user: User,
    date: LocalDate,
  ): F[Boolean] =
    if (date.atStartOfDay() > LocalDateTime.now())
      false.pure[F]
    else
      workSampleStore.wasAtWork(user, date.plusDays(1L).atStartOfDay())

  @tailrec
  private def collectGroupedWorkTime(
    list: List[(LocalDate, (List[WorkSample], Option[Holiday]))],
    user: User,
    wasAtWork: Boolean = false,
    result: Map[LocalDate, WorkTime] = Map.empty,
  ): Map[LocalDate, WorkTime] = list match {
    case Nil =>
      result
    case (date, (_, Some(_))) :: tail =>
      collectGroupedWorkTime(
        tail,
        user,
        wasAtWork = false,
        result + (date -> WorkTime.fromFiniteDuration((user.workingHours / 5).hour)),
      )
    case (date, (workSamples, _)) :: tail =>
      val (dayTime, newWasAtWork) = collectWorkTimeForWorkSamples(workSamples, wasAtWork, date)
      collectGroupedWorkTime(tail, user, newWasAtWork, result + (date -> dayTime))
  }
  @tailrec
  private def countTime(
    list: List[(LocalDate, (List[WorkSample], Option[Holiday]))],
    user: User,
    wasAtWork: Boolean = false,
    totalTime: WorkTime = WorkTime.empty,
  ): WorkTime = list match {
    case Nil =>
      totalTime
    case (_, (_, Some(_))) :: tail =>
      countTime(tail, user, wasAtWork = false, totalTime |+| WorkTime.fromFiniteDuration((user.workingHours / 5).hour))
    case (date, (workSamples, _)) :: tail =>
      val (dayTime, newWasAtWork) = collectWorkTimeForWorkSamples(workSamples, wasAtWork, date)
      countTime(tail, user, newWasAtWork, totalTime |+| dayTime)
  }

  private def collectWorkTimeForWorkSamples(
    workSamples: List[WorkSample],
    wasAtWork: Boolean,
    date: LocalDate,
  ): (WorkTime, Boolean) =
    if (workSamples.isEmpty && wasAtWork)
      (
        WorkTime.fromMillis(
          getNextAtStartOfDayInstant(date).toEpochMilli - date.toInstant().toEpochMilli,
        ),
        wasAtWork,
      )
    else
      countDayTime(workSamples, wasAtWork)

  @tailrec
  private def countDayTime(
    list: List[WorkSample],
    wasAtWork: Boolean = false,
    totalTime: WorkTime = WorkTime.empty,
  ): (WorkTime, Boolean) = list match {
    case Nil =>
      (totalTime, wasAtWork)
    case first :: second :: tail if first.activityType === Departure && second.activityType === Entrance =>
      countDayTime(
        tail,
        totalTime = totalTime |+| WorkTime.fromMillis(first.date.toEpochMilli - second.date.toEpochMilli),
      )
    case sample :: tail =>
      val date = sample.date.toLocalDate()
      val (newTotalTime, wasAtWork) = sample.activityType match {
        case Departure =>
          (WorkTime.fromMillis(sample.date.toEpochMilli - date.toInstant().toEpochMilli), true)
        case Entrance =>
          (WorkTime.fromMillis(getNextAtStartOfDayInstant(date).toEpochMilli - sample.date.toEpochMilli), false)
      }
      countDayTime(tail, wasAtWork, totalTime |+| newTotalTime)
  }

  @tailrec
  private def collectAllWorkIntervals(
    list: List[(LocalDate, (List[WorkSample], Option[Holiday]))],
    wasAtWork: Boolean = false,
    result: List[WorkInterval] = List.empty,
  ): List[WorkInterval] = list match {
    case Nil =>
      result
    case (date, (_, Some(_))) :: tail =>
      collectAllWorkIntervals(tail, wasAtWork = false, result :+ WorkInterval.Holiday(date))
    case (date, _) :: tail if date > LocalDate.now() =>
      collectAllWorkIntervals(tail, wasAtWork = false, result)
    case (date, (workSamples, _)) :: tail =>
      val (workIntervals, newWasAtWork): (List[WorkInterval], Boolean) =
        collectDayWorkIntervals(date, workSamples, wasAtWork, date.plusDays(1L).atStartOfDay())
      collectAllWorkIntervals(tail, newWasAtWork, result ++ workIntervals)
  }

  @tailrec
  private def collectDayWorkIntervals(
    date: LocalDate,
    list: List[WorkSample],
    wasAtWork: Boolean,
    lastDateTime: LocalDateTime,
    result: List[WorkInterval] = List.empty,
  ): (List[WorkInterval], Boolean) = list match {
    case _ if date === LocalDate.now() && lastDateTime > LocalDateTime.now() =>
      val now = LocalDateTime.now()
      collectDayWorkIntervals(date, list, wasAtWork, now, result)
    case Nil =>
      val fromDate = lastDateTime.toLocalDate.atStartOfDay()
      val workInterval = WorkInterval.FiniteWorkInterval(
        if (fromDate === lastDateTime) lastDateTime.toLocalDate.minusDays(1L).atStartOfDay() else fromDate,
        lastDateTime,
        wasAtWork,
      )
      (result :+ workInterval, wasAtWork)
    case sample :: tail if sample.activityType === Entrance =>
      val newLastDateTime = sample.date.toLocalDateTime()
      val workInterval = WorkInterval.FiniteWorkInterval(
        newLastDateTime,
        lastDateTime,
        wasAtWork = true,
      )
      collectDayWorkIntervals(date, tail, wasAtWork = false, newLastDateTime, result :+ workInterval)
    case sample :: tail if sample.activityType === Departure =>
      val newLastDateTime = sample.date.toLocalDateTime()
      val workInterval = WorkInterval.FiniteWorkInterval(
        newLastDateTime,
        lastDateTime,
        wasAtWork = false,
      )
      collectDayWorkIntervals(date, tail, wasAtWork = true, newLastDateTime, result :+ workInterval)
  }

  private def getNextAtStartOfDayInstant(date: LocalDate): Instant =
    date
      .plusDays(1)
      .toInstant()
      .min {
        ZonedDateTime.now().toInstant
      }

  private def extractWorkSamples(
    dateRange: List[LocalDate],
    groupedWorkSamples: Map[LocalDate, List[WorkSample]],
    holidays: Map[LocalDate, Holiday],
  ): List[(LocalDate, (List[WorkSample], Option[Holiday]))] =
    dateRange.map { date =>
      date -> (groupedWorkSamples.getOrElse(date, List.empty).reverse, holidays.get(date))
    }.reverse

  private def checkDateOrder(
    dateValidator: DateValidatorAlgebra[F],
    fromDate: LocalDate,
    toDate: LocalDate,
  ): EitherT[F, DateValidationError, Unit] =
    dateValidator.areDatesInProperOrder(fromDate.atStartOfDay(), toDate.atStartOfDay())
}

object WorkService {
  def apply[F[_]: Sync](
    userStore: UserStoreAlgebra[F],
    workSampleStore: WorkSampleStoreAlgebra[F],
    workSampleValidator: WorkSampleValidatorAlgebra[F],
    holidayStore: HolidayStoreAlgebra[F],
    dateValidatorAlgebra: DateValidatorAlgebra[F],
  ): WorkService[F] =
    new WorkService[F](userStore, workSampleStore, workSampleValidator, holidayStore, dateValidatorAlgebra)
}
