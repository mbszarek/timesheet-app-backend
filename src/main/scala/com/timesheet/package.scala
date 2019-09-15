package com

import monix.eval.Task
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.{ExecutionContext, Future}

package object timesheet {
  implicit def taskOps(task: Task[BSONCollection]): TaskOps = new TaskOps(task)

  final class TaskOps(private val task: Task[BSONCollection]) extends AnyVal {
    def executeOnCollection[K](action: ExecutionContext => BSONCollection => Future[K]): Task[K] = {
      task.flatMap { coll =>
        Task.deferFutureAction { implicit sc =>
          action(sc)(coll)
        }
      }
    }
  }

}
