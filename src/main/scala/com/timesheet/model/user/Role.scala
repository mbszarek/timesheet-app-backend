package com.timesheet.model.user

import cats._
import cats.implicits._
import com.avsystem.commons.serialization.{GenCodec, transparent}
import tsec.authorization.{AuthGroup, SimpleAuthEnum}

@transparent
final case class Role(roleRepr: String) {
  def isAdmin: Boolean = Role.adminRoles.contains(this)
}

object Role extends SimpleAuthEnum[Role, String] {
  implicit val Codec: GenCodec[Role] = GenCodec.materialize

  val Admin: Role    = Role("Admin")
  val Employer: Role = Role("Employer")
  val Worker: Role   = Role("Worker")

  val adminRoles: Set[Role] = Set(Admin, Employer)

  override val values: AuthGroup[Role] = AuthGroup(Admin, Employer, Worker)

  override def getRepr(t: Role): String = t.roleRepr

  implicit val eqRole: Eq[Role] = Eq.fromUniversalEquals[Role]
}
