package com.timesheet
package core.store.worksample.impl

import java.time.{LocalDateTime, ZoneOffset}

import cats._
import cats.implicits._
import cats.effect._
import cats.data.OptionT
import com.timesheet.core.db.MongoDriverMixin
import com.timesheet.core.store.worksample.WorkSampleStoreAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.user.{User, UserId}
import com.timesheet.model.work.{Departure, Entrance, WorkSample}
import org.mongodb.scala.MongoCollection
import fs2.interop.reactivestreams._
import org.mongodb.scala.model.Filters._

import scala.reflect.ClassTag

final class WorkSampleStoreMongo[F[_]: ConcurrentEffect] extends WorkSampleStoreAlgebra[F] with MongoDriverMixin[F] {
  override type T = WorkSample

  protected val collection: F[MongoCollection[WorkSample]] = getCollection("workSamples")

  def create(workSample: WorkSample): F[WorkSample] =
    for {
      coll <- collection
      _ <- coll
        .insertOne(workSample)
        .compileFS2
        .drain
    } yield workSample

  def update(workSample: WorkSample): OptionT[F, WorkSample] =
    OptionT.liftF {
      for {
        coll <- collection
        _ <- coll
          .findOneAndReplace(equal("_id", workSample.id.value), workSample)
          .compileFS2
          .drain
      } yield workSample
    }

  def get(id: ID): OptionT[F, WorkSample] =
    OptionT {
      for {
        coll <- collection
        workSample <- coll
          .find(equal("_id", id.value))
          .compileFS2
          .last
      } yield workSample
    }

  def getAllForUserBetweenDates(
    userId: UserId,
    from: LocalDateTime,
    to: LocalDateTime,
  ): F[List[WorkSample]] =
    for {
      coll <- collection
      workSamples <- coll
        .find(
          and(
            equal("userId", userId.value),
            and(gte("date", from.toInstant(ZoneOffset.UTC)), lte("date", to.toInstant(ZoneOffset.UTC))),
          ),
        )
        .compileFS2
        .toList
    } yield workSamples

  def wasAtWork(
    user: User,
    date: LocalDateTime,
  ): F[Boolean] =
    for {
      coll <- collection

      workSampleEarlier <- coll
        .find(
          and(
            equal("userId", user.id.value),
            lt("date", date.toInstant(ZoneOffset.UTC)),
          ),
        )
        .sort(equal("date", -1))
        .toFS2
        .head
        .compile
        .last

      workSampleLater <- coll
        .find(
          and(
            equal("userId", user.id.value),
            gt("date", date.toInstant(ZoneOffset.UTC)),
          ),
        )
        .sort(equal("date", 1))
        .toFS2
        .head
        .compile
        .last

    } yield {
      workSampleEarlier
        .map {
          _.activityType match {
            case Entrance  => true
            case Departure => false
          }
        }
        .orElse {
          workSampleLater.map {
            _.activityType match {
              case Entrance  => false
              case Departure => true
            }
          }
        }
        .getOrElse(user.isCurrentlyAtWork)
    }

  def getAll(): F[List[WorkSample]] =
    for {
      coll <- collection
      workSamples <- coll
        .find()
        .compileFS2
        .toList
    } yield workSamples

  def delete(id: ID): OptionT[F, WorkSample] =
    OptionT {
      for {
        coll <- collection
        workSample <- coll
          .findOneAndDelete(equal("_id", id.value))
          .asReactive
          .toStream[F]
          .compile
          .last
      } yield workSample
    }

  def getAllForUser(userId: UserId): F[List[WorkSample]] =
    for {
      coll <- collection
      workSamples <- coll
        .find(equal("userId", userId.value))
        .asReactive
        .toStream[F]
        .compile
        .toList
    } yield workSamples
}

object WorkSampleStoreMongo {
  def apply[F[_]: ConcurrentEffect]: WorkSampleStoreMongo[F] = new WorkSampleStoreMongo[F]()
}
