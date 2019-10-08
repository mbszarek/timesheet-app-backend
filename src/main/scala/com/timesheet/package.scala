package com

import cats.implicits._
import com.timesheet.concurrent.FutureConcurrentEffect
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.{ExecutionContext, Future}

package object timesheet {
  implicit def collectionTaskOps[F[_]: FutureConcurrentEffect](task: F[BSONCollection]): CollectionTaskOps[F] =
    new CollectionTaskOps(task)

  final class CollectionTaskOps[F[_]: FutureConcurrentEffect](collectionTask: F[BSONCollection]) {
    def executeOnCollection[K](action: ExecutionContext => BSONCollection => Future[K]): F[K] =
      for {
        collection <- collectionTask
        result     <- FutureConcurrentEffect[F].wrapFuture(implicit sc => action(sc)(collection))
      } yield result
  }

}
