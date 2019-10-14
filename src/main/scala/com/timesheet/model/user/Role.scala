package com.timesheet.model.user

import cats._
import cats.implicits._
import com.avsystem.commons.serialization.{GenCodec, transparent}
import reactivemongo.bson.{BSONDocumentHandler, Macros}
import tsec.authorization.{AuthGroup, SimpleAuthEnum}
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry

@transparent
final case class Role(roleRepr: String)

object Role extends SimpleAuthEnum[Role, String] {
  implicit val Codec: GenCodec[Role] = GenCodec.materialize

  val codecRegistry = fromRegistries()

  val Admin: Role    = Role("Admin")
  val Customer: Role = Role("Customer")

  implicit val roleHandler: BSONDocumentHandler[Role] = Macros.handler[Role]

  override val values: AuthGroup[Role] = AuthGroup(Admin, Customer)

  override def getRepr(t: Role): String = t.roleRepr

  implicit val eqRole: Eq[Role] = Eq.fromUniversalEquals[Role]
}
