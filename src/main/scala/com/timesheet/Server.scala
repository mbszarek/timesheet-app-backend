package com.timesheet

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import com.timesheet.auth.ExampleAuth
import fs2.Stream
import cats.implicits._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import monix.execution.Scheduler.Implicits.global

object Server {

  def stream[F[_]: ConcurrentEffect](implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {
    for {
      _ <- BlazeClientBuilder[F](global).stream
      helloWorldAlg = HelloWorld.impl[F]
      authedService = ExampleAuth[F]

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = (
        Routes.helloWorldRoutes[F](helloWorldAlg) <+>
          authedService.service
        ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(38080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
  }.drain
}
