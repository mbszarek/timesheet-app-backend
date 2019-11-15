package com.timesheet.model.rest.users

import com.timesheet.model.user.{Role, User, UserId}
import tsec.passwordhashers.PasswordHash
import cats.effect._
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe._

final case class SignupRequest(
  username: String,
  firstName: String,
  lastName: String,
  email: String,
  password: String,
  phone: String,
  role: Role,
  workingHours: Double,
) {
  implicit def decoder[F[_]: Sync]: EntityDecoder[F, SignupRequest] = jsonOf

  def asUser[A](hashedPassword: PasswordHash[A]): User = User(
    UserId.createNew(),
    username,
    firstName,
    lastName,
    email,
    hashedPassword.toString,
    phone,
    role,
    workingHours,
    ((workingHours / 40) * 26).toInt,
  )
}

object SignupRequest {
  implicit def decoder[F[_]: Sync]: EntityDecoder[F, SignupRequest] = jsonOf
}
