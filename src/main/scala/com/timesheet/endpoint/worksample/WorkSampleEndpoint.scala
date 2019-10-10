package com.timesheet.endpoint.worksample

import java.time.LocalDate

import cats.effect.Sync
import cats.implicits._
import com.timesheet.core.auth.Auth
import com.timesheet.core.service.user.UserServiceAlgebra
import com.timesheet.core.service.work.WorkServiceAlgebra
import com.timesheet.core.service.worksample.WorkSampleServiceAlgebra
import com.timesheet.core.validation.ValidationUtils.WorkSampleValidationError
import com.timesheet.endpoint.AuthEndpoint
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId
import com.timesheet.model.worksample.WorkSample
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Response}
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

class WorkSampleEndpoint[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  private def logWorkEndpoint(
    userService: UserServiceAlgebra[F],
    workService: WorkServiceAlgebra[F],
    workSampleService: WorkSampleServiceAlgebra[F],
  ): AuthEndpoint[F, Auth] = {
    case POST -> Root / "start" asAuthed user =>
      for {
        workSampleEither <- workSampleService.tagWorkerEntrance(user).value
        result           <- handleEitherToJson(workSampleEither)
      } yield result

    case POST -> Root / "end" asAuthed user =>
      for {
        workSampleEither <- workSampleService.tagWorkerExit(user).value
        result           <- handleEitherToJson(workSampleEither)
      } yield result

    case GET -> Root asAuthed user =>
      for {
        time   <- workService.collectDayWorkTimeForUser(user.id, LocalDate.now())
        result <- Ok(time.toString().asJson)
      } yield result

  }

  def endpoints(
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserServiceAlgebra[F],
    workService: WorkServiceAlgebra[F],
    workSampleService: WorkSampleServiceAlgebra[F],
  ): HttpRoutes[F] = {
    val allRolesRoutes = Auth.allRoles {
      logWorkEndpoint(userService, workService, workSampleService)
    }

    auth.liftService(allRolesRoutes)
  }

  private def handleEitherToJson(value: Either[WorkSampleValidationError, WorkSample]): F[Response[F]] =
    value.fold({ error =>
      BadRequest(error.asJson)
    }, { workSample =>
      Created(workSample.asJson)
    })
}

object WorkSampleEndpoint {
  def endpoint[F[_]: Sync, Auth: JWTMacAlgo](
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserServiceAlgebra[F],
    workService: WorkServiceAlgebra[F],
    workSampleService: WorkSampleServiceAlgebra[F],
  ): HttpRoutes[F] = new WorkSampleEndpoint[F, Auth].endpoints(auth, userService, workService, workSampleService)
}
