package com.timesheet.model.user

import cats.Applicative
import com.timesheet.model.user.User._
import reactivemongo.bson.Macros.Annotations.Key
import tsec.authorization.AuthorizationInfo

final case class User(
  @Key("_id") id: Option[UserId] = None,
  username: String,
  firstName: String,
  lastName: String,
  email: String,
  hash: String,
  phone: String,
  role: Role,
)

final case class AuthenticationError(
  error: String,
)

object User {
  final case class UserId(id: String)

  implicit def authRole[F[_]](implicit F: Applicative[F]): AuthorizationInfo[F, Role, User] =
    (u: User) => F.pure(u.role)
}
