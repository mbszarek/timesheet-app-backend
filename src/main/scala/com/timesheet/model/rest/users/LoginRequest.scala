package com.timesheet.model.rest.users

import org.http4s.EntityDecoder
import cats.effect._
import io.circe.generic.auto._
import org.http4s.circe._

final case class LoginRequest(
  username: String,
  password: String,
)

object LoginRequest {
  implicit def decoder[F[_]: Sync]: EntityDecoder[F, LoginRequest] = jsonOf
}
