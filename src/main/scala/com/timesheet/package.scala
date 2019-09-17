package com

import monix.eval.Task
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.{ExecutionContext, Future}

package object timesheet {
  implicit def collectionTaskOps(task: Task[BSONCollection]): CollectionTaskOps = new CollectionTaskOps(task)

  final class CollectionTaskOps(private val collectionTask: Task[BSONCollection]) extends AnyVal {
    def executeOnCollection[K](action: ExecutionContext => BSONCollection => Future[K]): Task[K] = {
      collectionTask.flatMap { coll =>
        Task.deferFutureAction { implicit sc =>
          action(sc)(coll)
        }
      }
    }
  }

}
