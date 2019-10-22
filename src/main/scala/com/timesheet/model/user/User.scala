package com.timesheet.model.user

import cats._
import cats.effect._
import cats.implicits._
import com.avsystem.commons.serialization.{GenCodec, name, transientDefault, transparent}
import com.timesheet.model.user.User._
import io.circe.generic.auto._
import org.bson.types.ObjectId
import org.http4s.EntityDecoder
import org.http4s.circe._
import tsec.authorization.AuthorizationInfo

final case class User(
  @name("_id") id: UserId,
  username: String,
  firstName: String,
  lastName: String,
  email: String,
  hash: String,
  phone: String,
  role: Role,
  @transientDefault isCurrentlyAtWork: Option[Boolean] = None
)

object User {
  implicit val Codec: GenCodec[User] = GenCodec.materialize

  @transparent
  final case class UserId(id: String)

  object UserId {
    implicit val Codec: GenCodec[UserId] = GenCodec.materialize

    def createNew(): UserId = UserId(ObjectId.get().toHexString)
  }

  implicit def authRole[F[_]: Applicative]: AuthorizationInfo[F, Role, User] =
    (u: User) => u.role.pure[F]

  implicit def userDecoder[F[_]: Sync]: EntityDecoder[F, User] = jsonOf
}
