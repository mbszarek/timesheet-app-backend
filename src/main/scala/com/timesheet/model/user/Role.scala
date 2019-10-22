package com.timesheet.model.user

import cats._
import cats.implicits._
import com.avsystem.commons.serialization.{GenCodec, transparent}
import tsec.authorization.{AuthGroup, SimpleAuthEnum}

@transparent
final case class Role(roleRepr: String)

object Role extends SimpleAuthEnum[Role, String] {
  implicit val Codec: GenCodec[Role] = GenCodec.materialize

  val Admin: Role    = Role("Admin")
  val Customer: Role = Role("Customer")

  override val values: AuthGroup[Role] = AuthGroup(Admin, Customer)

  override def getRepr(t: Role): String = t.roleRepr

  implicit val eqRole: Eq[Role] = Eq.fromUniversalEquals[Role]
}
