package com.timesheet.model.user

import cats._
import cats.implicits._
import com.avsystem.commons.serialization.{HasGenCodec, transparent}
import org.bson.types.ObjectId

@transparent
final case class UserId(value: String)

object UserId extends HasGenCodec[UserId] {
  def createNew(): UserId = UserId(ObjectId.get().toHexString)

  implicit def equalInstance: Eq[UserId] = Eq.fromUniversalEquals
}
