package com.timesheet
package core.store.base

import cats.effect._
import cats.implicits._
import cats.data._
import com.timesheet.core.db.MongoDriverMixin
import com.timesheet.model.db.{ID}
import org.mongodb.scala.model.Filters._

abstract class StoreAlgebraImpl[F[_]: ConcurrentEffect] extends StoreAlgebra[F] with MongoDriverMixin[F] {

  override type T = K

  override def create(entity: K): F[K] =
    for {
      coll <- collection
      _ <- coll
        .insertOne(entity)
        .compileFS2
        .drain
    } yield entity

  override def update(entity: K): OptionT[F, K] =
    OptionT.liftF {
      for {
        coll <- collection
        _ <- coll
          .findOneAndReplace(equal("_id", entity.id.value), entity)
          .compileFS2
          .drain
      } yield entity
    }

  override def get(id: ID): OptionT[F, K] =
    OptionT {
      for {
        coll <- collection
        entity <- coll
          .find(equal("_id", id.value))
          .compileFS2
          .last
      } yield entity
    }

  override def getAll(): F[List[K]] =
    for {
      coll <- collection
      entities <- coll
        .find()
        .compileFS2
        .toList
    } yield entities

  override def delete(id: ID): OptionT[F, K] =
    OptionT {
      for {
        coll <- collection
        entity <- coll
          .findOneAndDelete(equal("_id", id.value))
          .compileFS2
          .last
      } yield entity
    }
}
