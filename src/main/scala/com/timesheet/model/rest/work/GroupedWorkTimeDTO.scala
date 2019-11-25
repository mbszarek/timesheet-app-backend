package com.timesheet.model.rest.work

import cats.effect.Sync
import java.time.LocalDate

import org.http4s.circe._
import io.circe.generic.auto._
import org.http4s.EntityEncoder

final case class GroupedWorkTimeDTO(
  date: LocalDate,
  duration: Long,
)

object GroupedWorkTimeDTO {
  implicit def encoder[F[_]: Sync]: EntityEncoder[F, GroupedWorkTimeDTO] = jsonEncoderOf
}
