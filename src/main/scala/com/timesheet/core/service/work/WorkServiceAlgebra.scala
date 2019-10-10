package com.timesheet.core.service.work

import java.time.LocalDate

import cats.data._
import com.timesheet.model.user.User.UserId

import scala.concurrent.duration._

trait WorkServiceAlgebra[F[_]] {
  def collectDayWorkTimeForUser(userId: UserId, day: LocalDate): OptionT[F, FiniteDuration]
}
