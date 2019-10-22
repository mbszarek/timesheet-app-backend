package com

import com.avsystem.commons.mongo.async.MongoObservableExtensions
import com.github.ghik.silencer.silent
import com.timesheet.concurrent.FutureConcurrentEffect
import fs2.Stream
import fs2.interop.reactivestreams._

package object timesheet extends MongoObservableExtensions {
  @silent("deprecated")
  implicit def newMongoObservableOps[F[_]: FutureConcurrentEffect, T](
    obs: com.mongodb.async.client.Observable[T]
  ): MongoObservableOps[F, T] = new MongoObservableOps[F, T](obs)

  @silent("deprecated")
  final class MongoObservableOps[F[_]: FutureConcurrentEffect, T](obs: com.mongodb.async.client.Observable[T]) {
    def toFS2: Stream.CompileOps[F, F, T] =
      obs.asReactive
        .toStream[F]
        .compile
  }

}
