package com.timesheet

import cats.effect.ConcurrentEffect
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object Routes {

  def helloWorldRoutes[F[_]: ConcurrentEffect](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        for {
          greeting <- H.hello(HelloWorld.Name(name))
          resp     <- Ok(greeting)
        } yield resp
    }
  }
}
