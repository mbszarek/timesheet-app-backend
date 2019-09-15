package com.timesheet.model.user

import cats.Applicative
import com.timesheet.model.user.User._
import javax.xml.bind.DatatypeConverter
import reactivemongo.bson.{BSONDocument, BSONDocumentHandler, BSONHandler, BSONObjectID, Macros}
import reactivemongo.bson.Macros.Annotations.Key
import tsec.authorization.AuthorizationInfo

final case class User(
  @Key("_id") id: UserId,
  username: String,
  firstName: String,
  lastName: String,
  email: String,
  hash: String,
  phone: String,
  role: Role,
  isCurrentlyAtWork: Option[Boolean] = None
)

object User {
  final case class UserId(id: String)

  object UserId {
    def createNew(): UserId = UserId(BSONObjectID.generate().stringify)

    implicit val userIdHandler: BSONHandler[BSONObjectID, UserId] = new BSONHandler[BSONObjectID, UserId] {
      override def write(id: UserId): BSONObjectID =
        BSONObjectID(DatatypeConverter.parseHexBinary(id.id))

      override def read(bson: BSONObjectID): UserId = UserId(bson.stringify)
    }
  }

  implicit val userHandler: BSONDocumentHandler[User] = Macros.handler[User]

  implicit def authRole[F[_]](implicit F: Applicative[F]): AuthorizationInfo[F, Role, User] =
    (u: User) => F.pure(u.role)
}
