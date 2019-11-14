package com.timesheet
package core.store.worksample.impl

import java.time.{LocalDateTime, ZoneOffset}

import cats.effect._
import cats.implicits._
import com.timesheet.core.store.base.StoreAlgebraImpl
import com.timesheet.core.store.worksample.WorkSampleStoreAlgebra
import com.timesheet.model.user.{User, UserId}
import com.timesheet.model.work.{Departure, Entrance, WorkSample}
import fs2.interop.reactivestreams._
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters._

final class WorkSampleStoreMongo[F[_]: ConcurrentEffect] extends StoreAlgebraImpl[F] with WorkSampleStoreAlgebra[F] {

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
