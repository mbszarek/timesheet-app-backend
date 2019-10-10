package com.timesheet

import cats.effect.{ContextShift, Timer}
import com.timesheet.concurrent.FutureConcurrentEffect
import com.timesheet.core.auth.Auth
import com.timesheet.core.service.user.impl.UserService
import com.timesheet.core.service.work.impl.WorkService
import com.timesheet.core.service.worksample.impl.WorkSampleService
import com.timesheet.core.store.auth.AuthStoreMongo
import com.timesheet.core.store.user.impl.UserStoreMongo
import com.timesheet.core.store.worksample.impl.WorkSampleStoreMongo
import com.timesheet.core.validation.user.impl.UserValidator
import com.timesheet.core.validation.worksample.impl.WorkSampleValidator
import com.timesheet.endpoint.user.UserEndpoint
import com.timesheet.endpoint.worksample.WorkSampleEndpoint
import com.timesheet.endpoint.{HelloWorldEndpoint, TestEndpoint}
import com.timesheet.init.InitService
import fs2.Stream
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, Logger}
import tsec.authentication.SecuredRequestHandler
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt

class Server[F[_]: FutureConcurrentEffect] {
  def stream(implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {
    for {
      key <- Stream.eval(HMACSHA256.generateKey[F])
      authStore           = AuthStoreMongo[F, HMACSHA256](key)
      userStore           = UserStoreMongo[F]
      workSampleStore     = WorkSampleStoreMongo[F]
      userValidator       = UserValidator[F](userStore)
      workSampleValidator = WorkSampleValidator[F]
      userService         = UserService[F](userStore, userValidator)
      workService         = WorkService[F](workSampleStore)
      workSampleService   = WorkSampleService[F](userStore, workSampleStore, workSampleValidator)
      authenticator       = Auth.jwtAuthenticator[F, HMACSHA256](key, authStore, userStore)
      routeAuth           = SecuredRequestHandler(authenticator)
      passwordHasher      = BCrypt.syncPasswordHasher[F]
      initService         = InitService[F, BCrypt](passwordHasher, userService)

      _ <- Stream.eval(initService.init)

      httpApp = Router(
        "/users" -> UserEndpoint.endpoint[F, BCrypt, HMACSHA256](userService, passwordHasher, routeAuth),
        "/hello" -> HelloWorldEndpoint[F, HMACSHA256](routeAuth),
        "/test"  -> TestEndpoint[F],
        "/work"  -> WorkSampleEndpoint.endpoint[F, HMACSHA256](routeAuth, userService, workService, workSampleService)
      ).orNotFound

      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(38080, "0.0.0.0")
        .withHttpApp(CORS(finalHttpApp))
        .serve
    } yield exitCode
  }.drain
}

object Server {
  def apply[F[_]: FutureConcurrentEffect]: Server[F] = new Server[F]()
}
