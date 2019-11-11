package com.timesheet.endpoint.worksample

import java.time.LocalDate

import cats.effect._
import cats.implicits._
import com.timesheet.core.auth.Auth
import com.timesheet.core.service.user.UserServiceAlgebra
import com.timesheet.core.service.work.WorkServiceAlgebra
import com.timesheet.core.validation.ValidationUtils.WorkSampleValidationError
import com.timesheet.endpoint.AuthEndpoint
import com.timesheet.model.rest.work.GetWorkingTimeResult
import com.timesheet.model.user.{User, UserId}
import com.timesheet.model.work.WorkSample
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.{HttpRoutes, QueryParamDecoder, Response}
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

final class WorkEndpoint[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
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
        result <- collectWorkingTime(workService, user, date, date)
      } yield result

    case GET -> Root / "getForDate" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(toDate) asAuthed user =>
      collectWorkingTime(workService, user, fromDate, toDate)

    case GET -> Root / "getSamplesForDate" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(toDate) asAuthed user =>
      for {
        workSamples <- workService.getAllWorkSamplesBetweenDates(user.id, fromDate, toDate)
        result      <- Ok(workSamples.asJson)
      } yield result

    case GET -> Root / "getIntervalsForDate" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(toDate) asAuthed user =>
      extractWorkIntervals(workService)(user, fromDate, toDate)
  }

  private def otherUserLogWorkEndpoint(
    userService: UserServiceAlgebra[F],
    workService: WorkServiceAlgebra[F],
  ): AuthEndpoint[F, Auth] = {
    def withOtherUser(username: String)(fun: User => F[Response[F]]): F[Response[F]] =
      userService.getUserByUsername(username).value >>= {
        case Right(user) => fun(user)
        case Left(_)     => NotFound()
      }
    {
      case POST -> Root / "other" / username / "start" asAuthed _ =>
        withOtherUser(username) { user =>
          for {
            workSampleEither <- workService.tagWorkerEntrance(user).value
            result           <- handleEitherToJson(workSampleEither)
          } yield result
        }

      case POST -> Root / "other" / username / "end" asAuthed user =>
        withOtherUser(username) { user =>
          for {
            workSampleEither <- workService.tagWorkerExit(user).value
            result           <- handleEitherToJson(workSampleEither)
          } yield result
        }

      case GET -> Root / "other" / username / "getForToday" asAuthed _ =>
        withOtherUser(username) { user =>
          for {
            date   <- Sync[F].delay(LocalDate.now())
            result <- collectWorkingTime(workService, user, date, date)
          } yield result
        }

      case GET -> Root / "other" / username / "getForDate" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(
            toDate,
          ) asAuthed _ =>
        withOtherUser(username) { user =>
          collectWorkingTime(workService, user, fromDate, toDate)
        }
      case GET -> Root / "other" / username / "getSamplesForDate" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(
            toDate,
          ) asAuthed _ =>
        withOtherUser(username) { user =>
          for {
            workSamples <- workService.getAllWorkSamplesBetweenDates(user.id, fromDate, toDate)
            result      <- Ok(workSamples.asJson)
          } yield result
        }

      case GET -> Root / "other" / username / "getIntervalsForDate" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(
            toDate,
          ) asAuthed _ =>
        withOtherUser(username) { user =>
          extractWorkIntervals(workService)(user, fromDate, toDate)
        }
    }
  }

  def endpoints(
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserServiceAlgebra[F],
    workService: WorkServiceAlgebra[F],
  ): HttpRoutes[F] = {
    val allRolesRoutes = Auth.allRoles {
      logWorkEndpoint(userService, workService) orElse
      otherUserLogWorkEndpoint(userService, workService) // I don't know how to add it to "Admin routes only"
    }
    auth.liftService(allRolesRoutes)
  }

  private def handleEitherToJson(value: Either[WorkSampleValidationError, WorkSample]): F[Response[F]] =
    value
      .swap
      .map(error => BadRequest(error.asJson))
      .getOrElse(Created())

  private def collectWorkingTime(
    workService: WorkServiceAlgebra[F],
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[Response[F]] =
    for {
      workingTime           <- workService.collectWorkTimeForUserBetweenDates(user, fromDate, toDate)
      obligatoryWorkingTime <- workService.collectObligatoryWorkTimeForUser(user, fromDate, toDate)
      workingSeconds           = workingTime.toSeconds
      obligatoryWorkingSeconds = obligatoryWorkingTime.toSeconds
      result <- Ok(GetWorkingTimeResult(workingSeconds, obligatoryWorkingSeconds).asJson)
    } yield result

  private def extractWorkIntervals(
    workService: WorkServiceAlgebra[F],
  )(
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[Response[F]] =
    workService.getAllWorkIntervalsBetweenDates(user, fromDate, toDate).value >>= {
      case Right(workIntervals) => Ok(workIntervals.asJson)
      case Left(ex)             => BadRequest(ex.asJson)
    }
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
