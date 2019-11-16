package com.timesheet.model.rest.holidayrequest

import cats.effect.Sync
import java.time.LocalDate
import org.http4s.circe._
import io.circe.generic.auto._
import org.http4s.EntityDecoder

final case class DeleteHolidayRequestDTO(
  fromDate: LocalDate,
  toDate: LocalDate,
)

object DeleteHolidayRequestDTO {
  implicit def decoder[F[_]: Sync]: EntityDecoder[F, DeleteHolidayRequestDTO] = jsonOf
}
