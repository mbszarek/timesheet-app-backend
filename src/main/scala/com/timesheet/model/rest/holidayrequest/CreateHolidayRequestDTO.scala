package com.timesheet.model.rest.holidayrequest

import java.time.LocalDate

import cats.effect.Sync
import com.timesheet.model.holiday.HolidayType
import org.http4s.EntityDecoder
import io.circe.generic.auto._
import org.http4s.circe._

final case class CreateHolidayRequestDTO(
  fromDate: LocalDate,
  toDate: LocalDate,
  holidayType: HolidayType,
  description: String,
)

object CreateHolidayRequestDTO {
  implicit def decoder[F[_]: Sync]: EntityDecoder[F, CreateHolidayRequestDTO] = jsonOf
}
