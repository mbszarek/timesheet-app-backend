package com.timesheet.model.rest.users

import org.http4s.EntityDecoder
import cats.effect._
import io.circe.generic.auto._
import org.http4s.circe._

final case class LoginDTO(
  username: String,
  password: String,
)

object LoginDTO {
  implicit def decoder[F[_]: Sync]: EntityDecoder[F, LoginDTO] = jsonOf
}
