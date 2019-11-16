package com.timesheet.model.rest.holidayrequest

import java.time.LocalDate

import cats.effect.Sync
import org.http4s.circe._
import io.circe.generic.auto._
import org.http4s.EntityDecoder

final case class ApproveHolidayRequestDTO(
  username: String,
  fromDate: LocalDate,
  toDate: LocalDate,
)

object ApproveHolidayRequestDTO {
  implicit def decoder[F[_]: Sync]: EntityDecoder[F, ApproveHolidayRequestDTO] = jsonOf
}
