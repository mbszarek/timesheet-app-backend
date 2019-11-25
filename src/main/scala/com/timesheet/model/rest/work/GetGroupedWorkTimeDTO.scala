package com.timesheet.model.rest.work

import cats.effect.Sync
import org.http4s.circe._
import io.circe.generic.auto._
import org.http4s.EntityEncoder

final case class GetGroupedWorkTimeDTO(
  userName: String,
  groupedWorkTime: List[GroupedWorkTimeDTO],
)

object GetGroupedWorkTimeDTO {
  implicit def encoder[F[_]: Sync]: EntityEncoder[F, GetGroupedWorkTimeDTO] = jsonEncoderOf
}
