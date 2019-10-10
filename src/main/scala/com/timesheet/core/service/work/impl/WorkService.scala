package com.timesheet.core.service.work.impl

import java.time.{LocalDate, ZoneId, ZoneOffset}

import cats.effect._
import cats.implicits._
import com.timesheet.concurrent.FutureConcurrentEffect
import com.timesheet.core.service.work.WorkServiceAlgebra
import com.timesheet.core.store.worksample.WorkSampleStoreAlgebra
import com.timesheet.model.user.User
import com.timesheet.model.worksample.{Departure, Entrance, WorkSample}

import scala.annotation.tailrec
import scala.concurrent.duration._

class WorkService[F[_]: FutureConcurrentEffect](workSampleStore: WorkSampleStoreAlgebra[F])
    extends WorkServiceAlgebra[F] {
  override def collectDayWorkTimeForUser(userId: User.UserId, day: LocalDate): F[FiniteDuration] = {
    val fromDate = day.atStartOfDay()
    val toDate   = fromDate.plusDays(1)

    for {
      workSamples <- workSampleStore.getAllForUserBetweenDates(userId, fromDate, toDate)
      result      <- Sync[F].delay(countTime(workSamples.toList))
    } yield result
  }

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
          date
            .plusDays(1)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli - sample.date.toEpochMilli
      }).milli
      countTime(tail, newTotalTime)
  }
}

object WorkService {
  def apply[F[_]: FutureConcurrentEffect](workSampleStore: WorkSampleStoreAlgebra[F]): WorkService[F] =
    new WorkService[F](workSampleStore)
}
