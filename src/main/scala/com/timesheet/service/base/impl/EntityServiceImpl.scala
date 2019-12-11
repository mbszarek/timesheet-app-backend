package com.timesheet.service.base.impl

import cats.data.EitherT
import cats.effect.Sync
import com.timesheet.core.error.ValidationErrors.{EntityError, EntityNotFound}
import com.timesheet.model.db.ID
import com.timesheet.service.base.EntityServiceAlgebra

abstract class EntityServiceImpl[F[_]: Sync] extends EntityServiceAlgebra[F] {
  override def create(entity: Entity): F[Entity] =
    entityStore.create(entity)

  override def update(entity: Entity): EitherT[F, EntityError, Entity] =
    entityStore
      .update(entity)
      .toRight[EntityError](EntityNotFound(entity.id))

  override def get(id: ID): EitherT[F, EntityError, Entity] =
    entityStore
      .get(id)
      .toRight[EntityError](EntityNotFound(id))

  override def getAll(): F[List[Entity]] =
    entityStore.getAll()

  override def delete(id: ID): EitherT[F, EntityError, Entity] =
    entityStore
      .delete(id)
      .toRight[EntityError](EntityNotFound(id))
}
