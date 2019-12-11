package com.timesheet
package core.store.worksample.impl

import java.time.{LocalDateTime, ZoneOffset}

import cats.effect._
import cats.implicits._
import com.avsystem.commons.serialization.GenCodec
import com.timesheet.service.init.config.entities.MongoConfig
import com.timesheet.core.store.base.StoreAlgebraImpl
import com.timesheet.core.store.worksample.WorkSampleStoreAlgebra
import com.timesheet.model.user.{User, UserId}
import com.timesheet.model.work.{Departure, Entrance, WorkSample}
import fs2.interop.reactivestreams._
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters._

final class WorkSampleStoreMongo[F[_]: ConcurrentEffect](implicit protected val mongoConfig: MongoConfig)
    extends StoreAlgebraImpl[F]
    with WorkSampleStoreAlgebra[F] {
  import WorkSample._

  protected val collection: F[MongoCollection[WorkSample]] = getCollection("workSamples")

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
            userIdRef equal userId,
            and(
              dateRef gte from.toInstant(ZoneOffset.UTC),
              dateRef lte to.toInstant(ZoneOffset.UTC),
            ),
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
            userIdRef equal user.id,
            dateRef lt date.toInstant(ZoneOffset.UTC),
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
            userIdRef equal user.id,
            dateRef gt date.toInstant(ZoneOffset.UTC),
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

  def getAllForUser(userId: UserId): F[List[WorkSample]] =
    for {
      coll <- collection
      workSamples <- coll
        .find(userIdRef equal userId)
        .asReactive
        .toStream[F]
        .compile
        .toList
    } yield workSamples
}

object WorkSampleStoreMongo {
  def apply[F[_]: ConcurrentEffect](implicit mongoConfig: MongoConfig): WorkSampleStoreMongo[F] =
    new WorkSampleStoreMongo[F]()
}
