package com.timesheet

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import monix.eval.instances.CatsConcurrentForTask
import monix.execution.Scheduler.Implicits.global
import com.timesheet.core.auth.Auth
import com.timesheet.core.service.user.UserService
import com.timesheet.core.store.auth.{AuthStoreInMemory, AuthStoreMongo}
import com.timesheet.core.store.user.impl.UserStoreInMemory
import com.timesheet.core.validation.user.impl.UserValidator
import com.timesheet.endpoint.{HelloWorldEndpoint, TestEndpoint}
import com.timesheet.endpoint.user.UserEndpoint
import com.timesheet.init.InitService
import fs2.Stream
import monix.eval.Task
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import tsec.authentication.SecuredRequestHandler
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt

object Server {

  def stream(implicit T: Timer[Task], C: ContextShift[Task]): Stream[Task, Nothing] = {
    for {
      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      key <- Stream.eval(HMACSHA256.generateKey[Task])
      mongoAuthStore = AuthStoreMongo[HMACSHA256](key)
//      authStore      = AuthStoreInMemory[Task, HMACSHA256]
      userStore      = UserStoreInMemory[Task]
      userValidator  = UserValidator[Task](userStore)
      userService    = UserService[Task](userStore, userValidator)
      authenticator  = Auth.jwtAuthenticator[Task, HMACSHA256](key, mongoAuthStore, userStore)
      routeAuth      = SecuredRequestHandler(authenticator)
      passwordHasher = BCrypt.syncPasswordHasher[Task]
      initService    = InitService[Task, BCrypt](passwordHasher, userService)

      _ <- Stream.eval(initService.init)

      httpApp = Router(
        "/users" -> UserEndpoint.endpoint[Task, BCrypt, HMACSHA256](userService, passwordHasher, routeAuth),
        "/hello" -> HelloWorldEndpoint[Task, HMACSHA256](routeAuth),
        "/test"  -> TestEndpoint[Task],
      ).orNotFound
      // With Middlewares in place
      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

      exitCode <- BlazeServerBuilder[Task]
        .bindHttp(38080, "0.0.0.0")
        .withHttpApp(CORS(finalHttpApp))
        .serve
    } yield exitCode
  }.drain
}
