package com.timesheet.model.rest.users

import com.timesheet.model.user.{Role, User, UserId}
import tsec.passwordhashers.PasswordHash
import cats.effect._
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe._

final case class SignupDTO(
  username: String,
  firstName: String,
  lastName: String,
  email: String,
  password: String,
  phone: String,
  role: Role,
  workingHours: Double,
  holidaysPerYear: Int,
) {
  implicit def decoder[F[_]: Sync]: EntityDecoder[F, SignupDTO] = jsonOf

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
    holidaysPerYear,
  )
}

object SignupDTO {
  implicit def decoder[F[_]: Sync]: EntityDecoder[F, SignupDTO] = jsonOf
}
