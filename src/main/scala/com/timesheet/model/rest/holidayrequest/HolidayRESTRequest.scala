package com.timesheet.model.rest.holidayrequest

import java.time.LocalDate

import cats.effect.Sync
import com.timesheet.model.db.ID
import com.timesheet.model.holiday.HolidayType
import com.timesheet.model.holidayrequest.HolidayRequest
import com.timesheet.model.user.User
import org.http4s.EntityDecoder
import io.circe.generic.auto._
import org.http4s.circe._

final case class HolidayRESTRequest(
  fromDate: LocalDate,
  toDate: LocalDate,
  holidayType: HolidayType,
  description: String,
)

object HolidayRESTRequest {
  implicit def decoder[F[_]: Sync]: EntityDecoder[F, HolidayRESTRequest] = jsonOf
}
