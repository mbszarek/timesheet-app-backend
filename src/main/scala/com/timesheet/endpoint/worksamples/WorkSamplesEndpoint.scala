package com.timesheet.endpoint.worksamples

import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.effect.Sync
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityEncoder._
import com.timesheet.core.auth.Auth
import com.timesheet.core.service.user.UserService
import com.timesheet.core.service.worksamples.WorkSamplesService
import com.timesheet.endpoint.AuthEndpoint
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

class WorkSamplesEndpoint[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  private def logWorkEndpoint(
    userService: UserService[F],
    workSamplesService: WorkSamplesService[F],
  ): AuthEndpoint[F, Auth] = {
    case POST -> Root / "start" asAuthed user =>
      for {
        workSample <- workSamplesService.tagWorkerEntrance(user.id)
        result     <- Created(workSample.asJson)
      } yield result

    case POST -> Root / "end" asAuthed user =>
      for {
        workSample <- workSamplesService.tagWorkerExit(user.id)
        result     <- Created(workSample.asJson)
      } yield result
  }

  def endpoints(
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserService[F],
    workSamplesService: WorkSamplesService[F],
  ): HttpRoutes[F] = {
    val allRolesRoutes = Auth.allRoles {
      logWorkEndpoint(userService, workSamplesService)
    }

    auth.liftService(allRolesRoutes)
  }
}

object WorkSamplesEndpoint {
  def endpoint[F[_]: Sync, Auth: JWTMacAlgo](
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserService[F],
    workSamplesService: WorkSamplesService[F],
  ): HttpRoutes[F] = new WorkSamplesEndpoint[F, Auth].endpoints(auth, userService, workSamplesService)
}
