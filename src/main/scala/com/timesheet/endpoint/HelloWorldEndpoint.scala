package com.timesheet.endpoint

import cats.effect._
import com.timesheet.core.auth.Auth
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

class HelloWorldEndpoint[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  import HelloWorldEndpoint._

  private def helloEndpoint(): AuthEndpoint[F, Auth] = {
    case GET -> Root asAuthed user =>
      Ok(Greeting(user.role.roleRepr).asJson)
  }

  def endpoint(auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]]): HttpRoutes[F] = {
    val authEndpoint: AuthService[F, Auth] = Auth.allRolesHandler(helloEndpoint())(Auth.adminOnly(helloEndpoint()))
    auth.liftService(authEndpoint)
  }

}

object HelloWorldEndpoint {
  def apply[F[_]: Sync, Auth: JWTMacAlgo](
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]]
  ): HttpRoutes[F] = new HelloWorldEndpoint[F, Auth].endpoint(auth)

  final case class Greeting(greeting: String) extends AnyVal
}
