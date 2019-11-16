package com.timesheet.model.rest.holidayrequest

import cats.effect.Sync
import io.circe.generic.auto._
import org.http4s.circe._
import java.time.LocalDate

import org.http4s.EntityDecoder

final case class DenyHolidayRequestDTO(
  username: String,
  fromDate: LocalDate,
  toDate: LocalDate,
  reason: Option[String],
)

object DenyHolidayRequestDTO {
  implicit def decoder[F[_]: Sync]: EntityDecoder[F, DenyHolidayRequestDTO] = jsonOf
}
