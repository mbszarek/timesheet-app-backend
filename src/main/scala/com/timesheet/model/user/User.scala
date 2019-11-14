package com.timesheet.model.user

import cats._
import cats.effect._
import cats.implicits._
import com.avsystem.commons.mongo.BsonRef
import com.avsystem.commons.serialization.{HasGenCodec, name, transientDefault}
import com.timesheet.model.db.DBEntityCompanion
import io.circe.generic.auto._
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
  workingHours: Double,
  @transientDefault holidaysPerYear: Int = 26,
  isCurrentlyAtWork: Boolean = false,
)

object User extends HasGenCodec[User] with DBEntityCompanion[User] {
  implicit def authRole[F[_]: Applicative]: AuthorizationInfo[F, Role, User] =
    (u: User) => u.role.pure[F]

  implicit def userDecoder[F[_]: Sync]: EntityDecoder[F, User] = jsonOf

  val idRef: BsonRef[User, UserId]       = bson.ref(_.id)
  val usernameRef: BsonRef[User, String] = bson.ref(_.username)
}
