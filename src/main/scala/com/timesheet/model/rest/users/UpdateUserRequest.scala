package com.timesheet.model.rest.users

import com.timesheet.model.user.{Role, User}
import cats.effect._
import org.http4s.EntityDecoder
import org.http4s.circe._
import io.circe.generic.auto._

final case class UpdateUserRequest(
  firstName: Option[String],
  lastName: Option[String],
  email: Option[String],
  phone: Option[String],
  role: Option[Role],
  workingHours: Option[Double],
) {
  def updateUser(user: User): User =
    user.copy(
      firstName = firstName.getOrElse(user.firstName),
      lastName = lastName.getOrElse(user.lastName),
      email = email.getOrElse(user.email),
      phone = phone.getOrElse(user.phone),
      role = role.getOrElse(user.role),
      workingHours = workingHours.getOrElse(user.workingHours)
    )
}

object UpdateUserRequest {
  implicit def updateUserRequestDecoder[F[_]: Sync]: EntityDecoder[F, UpdateUserRequest] = jsonOf
}
