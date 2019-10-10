package com.timesheet.model.work

import scala.concurrent.duration.FiniteDuration

final case class WorkTime(
  days: Long,
  hours: Long,
  minutes: Long,
  seconds: Long,
  millis: Long,
)

object WorkTime {
  def fromFiniteDuration(finiteDuration: FiniteDuration): WorkTime = {
    import finiteDuration._

    WorkTime(
      days = toDays,
      hours = toHours - toDays * 24,
      minutes = toMinutes - toHours * 60,
      seconds = toSeconds - toMinutes * 60,
      millis = toMillis - toSeconds * 1000,
    )
  }
}
