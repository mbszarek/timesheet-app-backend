package com.timesheet.concurrent.impl

import com.timesheet.concurrent.FutureConcurrentEffect
import monix.eval.Task
import monix.eval.instances.CatsConcurrentEffectForTask
import monix.execution.Scheduler

import scala.concurrent.{ExecutionContext, Future}

class FutureConcurrentEffectForTask(implicit sc: Scheduler, opts: Task.Options)
    extends CatsConcurrentEffectForTask
    with FutureConcurrentEffect[Task] {

  override def wrapFuture[A](future: ExecutionContext => Future[A]): Task[A] =
    Task.deferFutureAction(sc => future(sc))
}

object FutureConcurrentEffectForTask {
  implicit def taskFutureEffect(
    implicit sc: Scheduler,
    opts: Task.Options = Task.defaultOptions,
  ): FutureConcurrentEffectForTask =
    new FutureConcurrentEffectForTask
}
