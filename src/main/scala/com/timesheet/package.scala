package com

import cats.implicits._
import com.avsystem.commons.mongo.async.MongoObservableExtensions
import com.avsystem.commons.mongo.async.MongoObservableExtensions.MongoObservableOps
import com.github.ghik.silencer.silent
import com.timesheet.concurrent.FutureConcurrentEffect
import fs2.Stream
import reactivemongo.api.BSONSerializationPack.Reader
import reactivemongo.api.Cursor
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument
import fs2.interop.reactivestreams._

import scala.concurrent.{ExecutionContext, Future}

package object timesheet extends MongoObservableExtensions {
  implicit def collectionTaskOps[F[_]: FutureConcurrentEffect](task: F[BSONCollection]): CollectionTaskOps[F] =
    new CollectionTaskOps(task)

  implicit def bsonCollectionOps[T](collection: BSONCollection): BSONCollectionOps =
    new BSONCollectionOps(collection)

  @silent("deprecated")
  implicit def newMongoObservableOps[F[_]: FutureConcurrentEffect, T](
    obs: com.mongodb.async.client.Observable[T]
  ): MongoObservableOps[F, T] = new MongoObservableOps[F, T](obs)

  final class CollectionTaskOps[F[_]: FutureConcurrentEffect](collectionTask: F[BSONCollection]) {
    def executeOnCollection[K](action: ExecutionContext => BSONCollection => Future[K]): F[K] =
      for {
        collection <- collectionTask
        result     <- FutureConcurrentEffect[F].wrapFuture(implicit sc => action(sc)(collection))
      } yield result
  }

  final class BSONCollectionOps(private val collection: BSONCollection) extends AnyVal {
    def findList[T](
      selector: BSONDocument
    )(implicit reader: Reader[T], executionContext: ExecutionContext): Future[List[T]] =
      collection.find(selector, None).cursor[T]().collect[List](-1, Cursor.FailOnError[List[T]]())

    def executeOnCollection[F[_]: FutureConcurrentEffect, K](
      action: ExecutionContext => BSONCollection => Future[K]
    ): F[K] =
      FutureConcurrentEffect[F].wrapFuture(implicit sc => action(sc)(collection))
  }

  @silent("deprecated")
  final class MongoObservableOps[F[_]: FutureConcurrentEffect, T](obs: com.mongodb.async.client.Observable[T]) {
    def toFS2: Stream.CompileOps[F, F, T] =
      obs.asReactive
        .toStream[F]
        .compile
  }

}
