package com.timesheet

import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}

object Main extends TaskApp {
  def run(args: List[String]): Task[ExitCode] =
    for {
      _ <- Server.stream[Task].compile.drain
    } yield ExitCode.Success
}
