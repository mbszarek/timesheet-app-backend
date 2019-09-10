package com.timesheet

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import com.timesheet.core.auth.Auth
import com.timesheet.core.service.user.UserService
import com.timesheet.core.store.auth.AuthStoreInMemory
import com.timesheet.core.store.user.impl.UserStoreInMemory
import com.timesheet.core.validation.user.impl.UserValidator
import com.timesheet.endpoint.{HelloWorldEndpoint, TestEndpoint}
import com.timesheet.endpoint.user.UserEndpoint
import com.timesheet.init.InitService
import fs2.Stream
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import tsec.authentication.SecuredRequestHandler
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt

object Server {

  def stream[F[_]: ConcurrentEffect](implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {
    for {
      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      key <- Stream.eval(HMACSHA256.generateKey[F])
      helloWorldAlg  = HelloWorld.impl[F]
      authStore      = AuthStoreInMemory[F, HMACSHA256]
      userStore      = UserStoreInMemory[F]
      userValidator  = UserValidator[F](userStore)
      userService    = UserService[F](userStore, userValidator)
      authenticator  = Auth.jwtAuthenticator[F, HMACSHA256](key, authStore, userStore)
      routeAuth      = SecuredRequestHandler(authenticator)
      passwordHasher = BCrypt.syncPasswordHasher[F]
      initService    = InitService[F, BCrypt](passwordHasher, userService)

      _ <- Stream.eval(initService.init)

      httpApp = Router(
        "/users" -> UserEndpoint.endpoint[F, BCrypt, HMACSHA256](userService, BCrypt.syncPasswordHasher[F], routeAuth),
        "/hello" -> HelloWorldEndpoint[F, HMACSHA256](routeAuth),
        "/test" -> TestEndpoint[F],
      ).orNotFound
      // With Middlewares in place
      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(38080, "0.0.0.0")
        .withHttpApp(CORS(finalHttpApp))
        .serve
    } yield exitCode
  }.drain
}
