package com.timesheet.endpoint.worksample

import cats.effect.Sync
import cats.implicits._
import com.timesheet.core.auth.Auth
import com.timesheet.core.error.ValidationErrors.WorkSampleValidationError
import com.timesheet.endpoint.AuthEndpoint
import com.timesheet.model.user.{User, UserId}
import com.timesheet.model.work.WorkSample
import com.timesheet.service.user.UserServiceAlgebra
import com.timesheet.service.work.WorkServiceAlgebra
import org.http4s.{HttpRoutes, Response}
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityEncoder._
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

final class AdminWorkEndpoint[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  private def adminLogWorkEndpoint(
    userService: UserServiceAlgebra[F],
    workService: WorkServiceAlgebra[F],
  ): AuthEndpoint[F, Auth] = {
    def withOtherUser(username: String)(fun: User => F[Response[F]]): F[Response[F]] =
      userService
        .getUserByUsername(username)
        .value >>= {
        case Right(user) => fun(user)
        case Left(_)     => NotFound()
      }

    {
      case POST -> Root / "start" / username asAuthed _ =>
        withOtherUser(username) { user =>
          workService
            .tagWorkerEntrance(user)
            .value >>= handleEitherToJson
        }

      case POST -> Root / "end" / username asAuthed _ =>
        withOtherUser(username) { user =>
          workService
            .tagWorkerExit(user)
            .value >>= handleEitherToJson
        }
    }
  }

  private def handleEitherToJson(value: Either[WorkSampleValidationError, WorkSample]): F[Response[F]] =
    value
      .swap
      .map(error => BadRequest(error.asJson))
      .getOrElse(Created())

  def endpoints(
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserServiceAlgebra[F],
    workService: WorkServiceAlgebra[F],
  ): HttpRoutes[F] = {
    val routes = Auth.employerAdminOnly {
      adminLogWorkEndpoint(userService, workService)
    }

    auth.liftService(routes)
  }
}

object AdminWorkEndpoint {
  def endpoint[F[_]: Sync, Auth: JWTMacAlgo](
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserServiceAlgebra[F],
    workService: WorkServiceAlgebra[F],
  ): HttpRoutes[F] = new AdminWorkEndpoint[F, Auth].endpoints(auth, userService, workService)
}
