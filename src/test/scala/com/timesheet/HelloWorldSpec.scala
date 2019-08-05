package com.timesheet

import cats.effect.IO
import monix.eval.Task
import org.http4s._
import org.http4s.implicits._
import monix.eval.Task._
import monix.execution.Scheduler
import org.specs2.matcher.MatchResult

class HelloWorldSpec extends org.specs2.mutable.Specification {

  implicit val scheduler = Scheduler.singleThread("test")

  "HelloWorld" >> {
    "return 200" >> {
      uriReturns200()
    }
    "return hello world" >> {
      uriReturnsHelloWorld()
    }
  }

  private[this] val retHelloWorld: Response[Task] = {
    val getHW      = Request[Task](Method.GET, uri"/hello/world")
    val helloWorld = HelloWorld.impl[Task]
    Routes.helloWorldRoutes(helloWorld).orNotFound(getHW).runSyncUnsafe()
  }

  private[this] def uriReturns200(): MatchResult[Status] =
    retHelloWorld.status must beEqualTo(Status.Ok)

  private[this] def uriReturnsHelloWorld(): MatchResult[String] =
    retHelloWorld.as[String].runSyncUnsafe() must beEqualTo("{\"message\":\"Hello, world\"}")
}
