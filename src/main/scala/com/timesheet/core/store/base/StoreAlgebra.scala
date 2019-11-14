package com.timesheet.core.store.base

import cats.data._
import com.avsystem.commons.serialization.GenCodec
import com.timesheet.model.db.{DBEntityWithID, ID}

import scala.reflect.ClassTag

trait StoreAlgebra[F[_]] {
  type K <: DBEntityWithID

  implicit protected def tag: ClassTag[K]
  implicit protected def codec: GenCodec[K]

  def create(entity: K): F[K]

  def update(entity: K): OptionT[F, K]

  def get(id: ID): OptionT[F, K]

  def getAll(): F[List[K]]

  def delete(id: ID): OptionT[F, K]
}
