package com.timesheet

import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}
import monix.execution.Scheduler

object Main extends TaskApp {
  import com.timesheet.concurrent.impl.FutureConcurrentEffectForTask._

  implicit val sc: Scheduler = scheduler

  def run(args: List[String]): Task[ExitCode] =
    for {
      _ <- Server[Task].stream.compile.drain
    } yield ExitCode.Success
}
