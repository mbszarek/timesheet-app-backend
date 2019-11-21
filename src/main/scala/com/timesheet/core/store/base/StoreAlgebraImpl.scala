package com.timesheet
package core.store.base

import cats.effect._
import cats.implicits._
import cats.data._
import com.timesheet.core.db.MongoDriverMixin
import com.timesheet.model.db.ID

abstract class StoreAlgebraImpl[F[_]: ConcurrentEffect] extends StoreAlgebra[F] with MongoDriverMixin[F] {

  override type T = Entity

  override def create(entity: Entity): F[Entity] =
    for {
      coll <- collection
      _ <- coll
        .insertOne(entity)
        .compileFS2
        .drain
    } yield entity

  override def update(entity: Entity): OptionT[F, Entity] =
    OptionT.liftF {
      for {
        coll <- collection
        _ <- coll
          .findOneAndReplace(idRef equal entity.id, entity)
          .compileFS2
          .drain
      } yield entity
    }

  override def get(id: ID): OptionT[F, Entity] =
    OptionT {
      for {
        coll <- collection
        entity <- coll
          .find(idRef equal id)
          .compileFS2
          .last
      } yield entity
    }

  override def getAll(): F[List[Entity]] =
    for {
      coll <- collection
      entities <- coll
        .find()
        .compileFS2
        .toList
    } yield entities

  override def delete(id: ID): OptionT[F, Entity] =
    OptionT {
      for {
        coll <- collection
        entity <- coll
          .findOneAndDelete(idRef equal id)
          .compileFS2
          .last
      } yield entity
    }
}
