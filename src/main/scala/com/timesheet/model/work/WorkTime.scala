package com.timesheet.model.work

import cats._
import scala.concurrent.duration.FiniteDuration

final case class WorkTime(
  days: Long,
  hours: Long,
  minutes: Long,
  seconds: Long,
  millis: Long,
) {
  require {
    millis < 1000 && seconds < 60 && minutes < 60 && hours < 24
  }

  def toSeconds: Long = ((days * 24 + hours) * 60 + minutes) * 60 + seconds
}

object WorkTime {
  def fromFiniteDuration(duration: FiniteDuration): WorkTime =
    WorkTime(
      duration.toDays,
      duration.toHours   % 24,
      duration.toMinutes % 60,
      duration.toSeconds % 60,
      duration.toMillis  % 1000,
    )

  def empty: WorkTime = WorkTime(0L, 0L, 0L, 0L, 0L)

  def fromMillis(millis: Long): WorkTime = {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours   = minutes / 60
    val days    = hours / 24

    WorkTime(
      days,
      hours   % 24,
      minutes % 60,
      seconds % 60,
      millis  % 1000,
    )
  }

  implicit val workTimeMonoid: Monoid[WorkTime] = Monoid.instance(
    {
      WorkTime.empty
    }, { (w1, w2) =>
      val allMillis  = w1.millis + w2.millis
      val allSeconds = w1.seconds + w2.seconds + allMillis / 1000
      val allMinutes = w1.minutes + w2.minutes + allSeconds / 60
      val allHours   = w1.hours + w2.hours + allMinutes / 60
      val allDays    = w1.days + w2.days + allHours / 24

      WorkTime(
        allDays,
        allHours   % 24,
        allMinutes % 60,
        allSeconds % 60,
        allMillis  % 1000,
      )
    },
  )
}
