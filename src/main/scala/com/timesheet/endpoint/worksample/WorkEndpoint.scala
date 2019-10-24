package com.timesheet.endpoint.worksample

import java.time.LocalDate

import cats.effect.Sync
import cats.implicits._
import com.timesheet.core.auth.Auth
import com.timesheet.core.service.user.UserServiceAlgebra
import com.timesheet.core.service.work.WorkServiceAlgebra
import com.timesheet.core.validation.ValidationUtils.WorkSampleValidationError
import com.timesheet.endpoint.AuthEndpoint
import com.timesheet.model.user.{User, UserId}
import com.timesheet.model.rest.work.{GetWorkingTimeResult, WorkTime}
import com.timesheet.model.worksample.WorkSample
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.{HttpRoutes, QueryParamDecoder, Response}
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

class WorkEndpoint[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  import WorkEndpoint._

  private def logWorkEndpoint(
    userService: UserServiceAlgebra[F],
    workService: WorkServiceAlgebra[F],
  ): AuthEndpoint[F, Auth] = {
    case POST -> Root / "start" asAuthed user =>
      for {
        workSampleEither <- workService.tagWorkerEntrance(user).value
        result           <- handleEitherToJson(workSampleEither)
      } yield result

    case POST -> Root / "end" asAuthed user =>
      for {
        workSampleEither <- workService.tagWorkerExit(user).value
        result           <- handleEitherToJson(workSampleEither)
      } yield result

    case GET -> Root / "getForToday" asAuthed user =>
      for {
        date   <- Sync[F].delay(LocalDate.now())
        result <- collectWorkingTime(workService, user, date, date.plusDays(1))
      } yield result

    case GET -> Root / "getForDate" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(toDate) asAuthed user =>
      collectWorkingTime(workService, user, fromDate, toDate)

    case GET -> Root / "getSamplesForDate" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(toDate) asAuthed user =>
      for {
        workSamples <- workService.getAllWorkSamplesBetweenDates(user.id, fromDate, toDate)
        result      <- Ok(workSamples.asJson)
      } yield result
  }

  def endpoints(
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserServiceAlgebra[F],
    workService: WorkServiceAlgebra[F],
  ): HttpRoutes[F] = {
    val allRolesRoutes = Auth.allRoles {
      logWorkEndpoint(userService, workService)
    }

    auth.liftService(allRolesRoutes)
  }

  private def handleEitherToJson(value: Either[WorkSampleValidationError, WorkSample]): F[Response[F]] =
    value.swap
      .map(error => BadRequest(error.asJson))
      .getOrElse(Created())

  private def collectWorkingTime(
    workService: WorkServiceAlgebra[F],
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate
  ): F[Response[F]] =
    for {
      workingTime           <- workService.collectWorkTimeForUserBetweenDates(user.id, fromDate, toDate)
      obligatoryWorkingTime <- workService.collectObligatoryWorkTimeForUser(user, fromDate, toDate)
      workingHours           = WorkTime.fromFiniteDuration(workingTime)
      obligatoryWorkingHours = WorkTime.fromFiniteDuration(obligatoryWorkingTime)
      result <- Ok(GetWorkingTimeResult(workingHours, obligatoryWorkingHours).asJson)
    } yield result

}

object WorkEndpoint {
  def endpoint[F[_]: Sync, Auth: JWTMacAlgo](
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserServiceAlgebra[F],
    workService: WorkServiceAlgebra[F],
  ): HttpRoutes[F] = new WorkEndpoint[F, Auth].endpoints(auth, userService, workService)

  implicit val dateQueryParamDecoder: QueryParamDecoder[LocalDate] =
    QueryParamDecoder[String].map(LocalDate.parse)

  object FromLocalDateMatcher extends QueryParamDecoderMatcher[LocalDate]("from")

  object ToLocalDateMatcher extends QueryParamDecoderMatcher[LocalDate]("to")
}
