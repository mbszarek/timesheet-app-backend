package com

import cats.implicits._
import com.timesheet.concurrent.FutureConcurrentEffect
import reactivemongo.api.BSONSerializationPack.Reader
import reactivemongo.api.Cursor
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument

import scala.concurrent.{ExecutionContext, Future}

package object timesheet {
  implicit def collectionTaskOps[F[_]: FutureConcurrentEffect](task: F[BSONCollection]): CollectionTaskOps[F] =
    new CollectionTaskOps(task)

  implicit def bsonCollectionOps[T](collection: BSONCollection): BSONCollectionOps =
    new BSONCollectionOps(collection)

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

}
