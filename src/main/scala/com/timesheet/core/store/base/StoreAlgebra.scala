package com.timesheet.core.store.base

import cats.data._
import com.avsystem.commons.mongo.BsonRef
import com.avsystem.commons.serialization.GenCodec
import com.timesheet.model.db.{DBEntityWithID, ID}

import scala.reflect.ClassTag

trait StoreAlgebra[F[_]] {
  type Entity <: DBEntityWithID

  implicit protected def tag: ClassTag[Entity]
  implicit protected def codec: GenCodec[Entity]
  implicit protected def idRef: BsonRef[Entity, ID]

  def create(entity: Entity): F[Entity]

  def update(entity: Entity): OptionT[F, Entity]

  def get(id: ID): OptionT[F, Entity]

  def getAll(): F[List[Entity]]

  def delete(id: ID): OptionT[F, Entity]
}
