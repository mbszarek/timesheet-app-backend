package com.timesheet.model.rest.work

import cats.effect.Sync
import java.time.Instant

import org.http4s.circe._
import io.circe.generic.auto._
import com.timesheet.model.work.{ActivityType, WorkSample}
import org.http4s.EntityEncoder

final case class WorkSampleDTO(
  activityType: ActivityType,
  date: Instant,
)

object WorkSampleDTO {
  implicit def encoder[F[_]: Sync]: EntityEncoder[F, WorkSampleDTO] = jsonEncoderOf

  def fromWorkSample(workSample: WorkSample): WorkSampleDTO =
    WorkSampleDTO(
      workSample.activityType,
      workSample.date,
    )
}
