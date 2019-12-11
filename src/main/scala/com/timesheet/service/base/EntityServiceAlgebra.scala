package com.timesheet.service.base

import cats.data.EitherT
import com.timesheet.core.error.ValidationErrors.EntityError
import com.timesheet.core.store.base.StoreAlgebra
import com.timesheet.model.db.{DBEntityWithID, ID}

trait EntityServiceAlgebra[F[_]] { self =>
  type Entity <: DBEntityWithID

  protected def entityStore: EntityServiceAlgebra.EntityStore[F, Entity]

  def create(entity: Entity): F[Entity]

  def update(entity: Entity): EitherT[F, EntityError, Entity]

  def get(id: ID): EitherT[F, EntityError, Entity]

  def getAll(): F[List[Entity]]

  def delete(id: ID): EitherT[F, EntityError, Entity]
}

object EntityServiceAlgebra {
  type EntityStore[F[_], T] = StoreAlgebra[F] { type Entity = T }
}
