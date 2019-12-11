package com.timesheet
package service.workReport.impl

import java.io.{BufferedWriter, File, FileWriter}
import java.time.{Instant, LocalDate, LocalDateTime}
import java.util.UUID

import cats.data.{EitherT, Nested}
import cats.effect._
import cats.implicits._
import com.timesheet.core.error.ValidationErrors.DateValidationError
import com.timesheet.service.holiday.HolidayServiceAlgebra
import com.timesheet.service.work.WorkServiceAlgebra
import com.timesheet.service.workReport.WorkReportServiceAlgebra
import com.timesheet.model.holiday.Holiday
import com.timesheet.model.user.User
import com.timesheet.model.work.{Departure, Entrance, WorkSample, WorkTime}
import com.timesheet.util.DateRangeGenerator

final class WorkReportService[F[_]: Sync](
  workService: WorkServiceAlgebra[F],
  holidayService: HolidayServiceAlgebra[F],
) extends WorkReportServiceAlgebra[F] {
  def createReport[T](
    user: User,
    from: LocalDate,
    to: LocalDate,
  )(
    action: Either[DateValidationError, File] => F[T],
  ): F[T] =
    getFile(user).use { file =>
      Nested {
        {
          for {
            holidays    <- holidayService.collectHolidaysForUserBetweenDates(user.id, from, to)
            workSamples <- workService.getAllWorkSamplesBetweenDates(user.id, from, to)
            groupedWorkSamples = workSamples.groupBy(_.date.toLocalDate()).toList
            workTime <- workService.getWorkTimeForUserGroupedByDate(user, from, to)
            (totalWorkTime, totalObligatoryTime) = workTime.values.foldLeft((WorkTime.empty, WorkTime.empty)) {
              case ((totalWork, totalObligatory), (work, obligatory)) =>
                (totalWork |+| work, totalObligatory |+| obligatory)
            }
            printableHolidays <- EitherT.right[DateValidationError](
              holidays.sortBy(_.fromDate).flatTraverse(formatHoliday),
            )

            formattedWorkDays = groupedWorkSamples.sortBy(_._1).map {
              case (date, workSamples) => formatWorkDay(date, workTime(date)._1, workSamples)
            }
          } yield StringContext.processEscapes(
            s"""
             |${user.firstName} ${user.lastName}
             |${withIndentation(1)(s"""required time: ${withIndentation(1)(formatWorkTime(totalObligatoryTime))}""")}
             |${withIndentation(1)(s"""work time: ${withIndentation(1)(formatWorkTime(totalWorkTime))}""")}
             |${withIndentation(level = 1)(s"""time difference: ${withIndentation(1)(
                 formatWorkTime(totalObligatoryTime.workTimeDifference(totalWorkTime)),
               )}""")}
             |------------
             |
             |${withIndentation(0)(s"""Holidays taken (days): ${withIndentation(1)(s"${printableHolidays.size}")}""")}
             |${printableHolidays.map(withIndentation(2)).mkString("\n")}
             |------------
             |
             |Details:
             |${formattedWorkDays.mkString("\n")}
             |""".stripMargin,
          )
        }.value
      }.map { string =>
        val bw = new BufferedWriter(new FileWriter(file))
        bw.write(string)
        bw.close()
        file
      }.value >>= action
    }

  private def getFile(user: User): Resource[F, File] =
    Resource.make {
      Sync[F].delay(File.createTempFile(s"${user.username}-${UUID.randomUUID().toString}", ".txt"))
    } { file =>
      Sync[F]
        .delay(file.deleteOnExit())
    }

  private def formatWorkTime(workTime: WorkTime): String = {
    val seconds = workTime.toSeconds % 60
    val minutes = ((workTime.toSeconds - seconds) / 60) % 60
    val hours   = ((workTime.toSeconds - seconds) / 60 - minutes) / 60
    s"$hours:$minutes:$seconds"
  }

  private def formatInstant(instant: Instant): String = {
    val dateTime = instant.toLocalDateTime()
    formatLocalDateTime(dateTime)
  }

  private def formatLocalDateTime(date: LocalDateTime): String =
    s"""${date.getHour}:${date.getMinute}:${date.getSecond}"""

  private def formatLocalDate(date: LocalDate): String =
    s"""${date.getYear}-${date.getMonthValue}-${date.getDayOfMonth}"""

  private def formatWorkDay(
    date: LocalDate,
    dateWorkTime: WorkTime,
    workSamples: List[WorkSample],
  ): String = {
    val dateFormat           = formatLocalDate(date)
    val workSamplesFormatted = workSamples.map(formatWorkSample)
    s"""
       |\t$dateFormat\twork time: ${formatWorkTime(dateWorkTime)}
       |${workSamplesFormatted.mkString("\n")}
       |""".stripMargin
  }

  private def formatWorkSample(workSample: WorkSample): String = {
    val dateTime = formatInstant(workSample.date)
    val activityTypeFormat = workSample.activityType match {
      case Entrance  => s"""Entrance"""
      case Departure => s"""Departure"""
    }
    withIndentation(2)(s"""$activityTypeFormat:${withIndentation(1)(dateTime)}""")
  }

  private def formatHoliday(holiday: Holiday): F[List[String]] = {
    for {
      date <- DateRangeGenerator[F].getDateStream(holiday.fromDate, holiday.toDate)
    } yield StringContext.processEscapes(s"""${formatLocalDate(date)}\tHoliday type: ${holiday.holidayType.value}""")
  }.compile.toList

  private def withIndentation(level: Int)(message: String): String =
    s"""${"\t" * level}$message"""
}

object WorkReportService {
  def apply[F[_]: Sync](
    workService: WorkServiceAlgebra[F],
    holidayService: HolidayServiceAlgebra[F],
  ): WorkReportService[F] = new WorkReportService[F](workService, holidayService)
}
