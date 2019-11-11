package com

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}

import cats.effect._
import com.avsystem.commons.mongo.async.MongoObservableExtensions
import com.github.ghik.silencer.silent
import fs2.Stream
import fs2.interop.reactivestreams._

package object timesheet extends MongoObservableExtensions {
  @silent("deprecated")
  implicit def newMongoObservableOps[F[_]: ConcurrentEffect, T](
    obs: com.mongodb.async.client.Observable[T],
  ): MongoObservableOps[F, T] = new MongoObservableOps[F, T](obs)

  implicit def instantOps(instant: Instant): InstantOps = new InstantOps(instant)

  implicit def localDateOps(localDate: LocalDate): LocalDateOps = new LocalDateOps(localDate)

  @silent("deprecated")
  final class MongoObservableOps[F[_]: ConcurrentEffect, T](obs: com.mongodb.async.client.Observable[T]) {
    def toFS2: Stream[F, T] =
      obs
        .asReactive
        .toStream[F]

    def compileFS2: Stream.CompileOps[F, F, T] =
      obs.toFS2.compile
  }

  final class InstantOps(private val instant: Instant) extends AnyVal {
    def toLocalDate(): LocalDate =
      instant
        .atZone(ZoneId.systemDefault())
        .toLocalDate

    def toLocalDateTime(): LocalDateTime =
      instant
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime
  }

  final class LocalDateOps(private val localDate: LocalDate) extends AnyVal {
    def toInstant(): Instant =
      localDate
        .atStartOfDay()
        .atZone(ZoneId.systemDefault())
        .toInstant

    def toLocalDateTime(): LocalDateTime =
      localDate
        .atStartOfDay()
  }

}
