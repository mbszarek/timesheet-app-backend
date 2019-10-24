package com.timesheet.model.login

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
  def asUser[A](hashedPassword: PasswordHash[A]): User = User(
    UserId.createNew(),
    username,
    firstName,
    lastName,
    email,
    hashedPassword.toString,
    phone,
    role = role,
    workingHours,
    isCurrentlyAtWork = None,
  )
}

object SignupRequest {
  implicit def signupRequestDecoder[F[_]: Sync]: EntityDecoder[F, SignupRequest] = jsonOf
}
