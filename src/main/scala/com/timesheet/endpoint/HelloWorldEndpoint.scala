package com.timesheet.endpoint

import cats._
import cats.effect._
import cats.implicits._
import com.timesheet.core.auth.Auth
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId
import io.circe.{Encoder, Json}
import org.http4s.{EntityEncoder, HttpRoutes}
import org.http4s.circe.jsonEncoderOf
import org.http4s.dsl.Http4sDsl
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.authentication._

class HelloWorldEndpoint[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  import HelloWorldEndpoint._

  private def helloEndpoint(): AuthEndpoint[F, Auth] = {
    case GET -> Root asAuthed user =>
      Ok(Greeting(user.role.roleRepr))
  }

  def endpoint(auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]]): HttpRoutes[F] = {
    val authEndpoint: AuthService[F, Auth] = Auth.allRolesHandler(helloEndpoint())(Auth.adminOnly(helloEndpoint()))
    val x = Auth.adminOnly(helloEndpoint())
    auth.liftService(x)
  }

}

object HelloWorldEndpoint {
  def apply[F[_]: Sync, Auth: JWTMacAlgo](
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]]
  ): HttpRoutes[F] = new HelloWorldEndpoint[F, Auth].endpoint(auth)

  final case class Name(name: String) extends AnyVal

  final case class Greeting(greeting: String) extends AnyVal

  implicit val greetingEncoder: Encoder[Greeting] = (a: Greeting) => Json.obj(
    ("message", Json.fromString(a.greeting)),
  )
  implicit def greetingEntityEncoder[F[_]: Applicative]: EntityEncoder[F, Greeting] =
    jsonEncoderOf[F, Greeting]
}
