package com.timesheet.auth

import cats.Applicative
import cats.data._
import cats.effect._
import cats.implicits._
import monix.eval.Task
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._

object ExampleAuth {
  final case class User(id: Long, name: String)

  def apply[F[_]: ConcurrentEffect]: AuthUser[F] = new AuthUser[F] {}

  abstract class AuthUser[F[_]: ConcurrentEffect] {
    val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._
    var cond = true

    val authedRoutes: AuthedRoutes[User, F] = AuthedRoutes.of {
      case GET -> Root / "welcome" as user =>
        Ok(s"Welcome, ${user.name}")
    }

    val userMap: Map[Long, User] = Map(1L -> User(1, "Kacper Stopa"), 2L -> User(2, "Seba Kozlak"))

    def retrieveUser: Kleisli[F, Long, Either[String, User]] =
      Kleisli(userMap.get(_).toRight("Cannot find user with this token.").pure[F])

    val authUser: Kleisli[F, Request[F], Either[String, User]] = Kleisli { request =>
      {
        for {
          header  <- headers.Cookie.from(request.headers).toRight("Cookie parsing error")
          cookie  <- header.values.toList.find(_.name == "authcookie").toRight("Couldn't find the authcookie")
          message <- Either.catchOnly[NumberFormatException](cookie.content.toLong).leftMap(_.toString)
        } yield message
      }.flatTraverse(retrieveUser.run)
    }

    private val onFailure: AuthedRoutes[String, F]  = Kleisli(req => OptionT.liftF(Forbidden(req.authInfo)))
    private val middleware: AuthMiddleware[F, User] = AuthMiddleware(authUser, onFailure)

    def service: HttpRoutes[F] = middleware(authedRoutes)
  }
}
