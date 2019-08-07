package com.timesheet.model.user

import cats.Applicative
import com.timesheet.model.user.User._
import tsec.authorization.AuthorizationInfo

final case class User(
  id: Option[UserId] = None,
  userName: String,
  firstName: String,
  lastName: String,
  email: String,
  hash: String,
  phone: String,
  role: Role,
)

object User {
  final case class UserId(id: Long)

  implicit def authRole[F[_]](implicit F: Applicative[F]): AuthorizationInfo[F, Role, User] =
    new AuthorizationInfo[F, Role, User] {
      override def fetchInfo(u: User): F[Role] = F.pure(u.role)
    }
}
