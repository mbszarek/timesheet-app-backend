package com.timesheet.model.rest.users

import cats.effect.Sync
import io.circe.generic.auto._
import org.http4s.circe._
import com.timesheet.model.user.{Role, User, UserId}
import org.http4s.EntityEncoder

final case class UserDTO(
  id: UserId,
  username: String,
  firstName: String,
  lastName: String,
  email: String,
  phone: String,
  role: Role,
  workingHours: Double,
  holidaysPerYear: Int,
  isCurrentlyAtWork: Boolean = false,
)

object UserDTO {
  implicit def encoder[F[_]: Sync]: EntityEncoder[F, UserDTO] = jsonEncoderOf

  def fromUser(user: User): UserDTO =
    UserDTO(
      user.id,
      user.username,
      user.firstName,
      user.lastName,
      user.email,
      user.phone,
      user.role,
      user.workingHours,
      user.holidaysPerYear,
      user.isCurrentlyAtWork,
    )
}
