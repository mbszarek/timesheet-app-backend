package com.timesheet.model.user

import cats._
import cats.implicits._
import reactivemongo.bson.{BSONDocumentHandler, Macros}
import tsec.authorization.{AuthGroup, SimpleAuthEnum}

final case class Role(roleRepr: String)

object Role extends SimpleAuthEnum[Role, String] {
  val Admin: Role    = Role("Admin")
  val Customer: Role = Role("Customer")

  implicit val roleHandler: BSONDocumentHandler[Role] = Macros.handler[Role]

  override val values: AuthGroup[Role] = AuthGroup(Admin, Customer)

  override def getRepr(t: Role): String = t.roleRepr

  implicit val eqRole: Eq[Role] = Eq.fromUniversalEquals[Role]
}
