package com.timesheet.endpoint.worksample

import java.time.LocalDate

import cats.effect._
import cats.implicits._
import com.timesheet.EndpointUtils._
import com.timesheet.core.auth.Auth
import com.timesheet.core.service.user.UserServiceAlgebra
import com.timesheet.core.service.work.WorkServiceAlgebra
import com.timesheet.core.validation.ValidationUtils.WorkSampleValidationError
import com.timesheet.endpoint.AuthEndpoint
import com.timesheet.model.rest.work.{GetGroupedWorkTimeDTO, GetWorkingTimeResultDTO, GroupedWorkTimeDTO, WorkSampleDTO}
import com.timesheet.model.user.{User, UserId}
import com.timesheet.model.work.WorkSample
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Response}
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo
import fs2._

final class WorkEndpoint[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {

  private def logWorkEndpoint(workService: WorkServiceAlgebra[F]): AuthEndpoint[F, Auth] = {
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
      workService
        .getAllWorkSamplesBetweenDates(user.id, fromDate, toDate)
        .value >>= {
        case Right(workSamples) =>
          Ok {
            workSamples
              .map(WorkSampleDTO.fromWorkSample)
              .asJson
          }
        case Left(ex) =>
          BadRequest(ex.asJson)
      }

    case GET -> Root / "getIntervalsForDate" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(toDate) asAuthed user =>
      extractWorkIntervals(workService)(user, fromDate, toDate)

    case GET -> Root / "getGroupedWorkTime" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(toDate) asAuthed user =>
      workService
        .getWorkTimeForUserGroupedByDate(user, fromDate, toDate)
        .value >>= {
        case Right(workMap) =>
          for {
            groupedWorkTimes <- Stream(workMap.toList: _*)
              .covary[F]
              .map {
                case (date, workTime) => GroupedWorkTimeDTO(date, workTime.toSeconds)
              }
              .compile
              .toList

            result <- Ok {
              GetGroupedWorkTimeDTO(user.username, groupedWorkTimes.sortBy(_.date)).asJson
            }
          } yield result

        case Left(ex) =>
          BadRequest(ex.asJson)
      }
  }

  private def otherUserLogWorkEndpoint(
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
      case POST -> Root / "other" / username / "start" asAuthed _ =>
        withOtherUser(username) { user =>
          for {
            workSampleEither <- workService.tagWorkerEntrance(user).value
            result           <- handleEitherToJson(workSampleEither)
          } yield result
        }

      case POST -> Root / "other" / username / "end" asAuthed _ =>
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
          workService
            .getAllWorkSamplesBetweenDates(user.id, fromDate, toDate)
            .value >>= {
            case Right(workSamples) =>
              Ok {
                workSamples
                  .map(WorkSampleDTO.fromWorkSample)
                  .asJson
              }
            case Left(ex) =>
              BadRequest(ex.asJson)
          }
        }

      case GET -> Root / "other" / username / "getIntervalsForDate" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(
            toDate,
          ) asAuthed _ =>
        withOtherUser(username) { user =>
          extractWorkIntervals(workService)(user, fromDate, toDate)
        }

      case GET -> Root / "other" / username / "getGroupedWorkTime" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(
            toDate,
          ) asAuthed _ =>
        withOtherUser(username) { user =>
          workService
            .getWorkTimeForUserGroupedByDate(user, fromDate, toDate)
            .value >>= {
            case Right(workMap) =>
              for {
                groupedWorkTimes <- Stream(workMap.toList: _*)
                  .covary[F]
                  .map {
                    case (date, workTime) => GroupedWorkTimeDTO(date, workTime.toSeconds)
                  }
                  .compile
                  .toList

                result <- Ok {
                  GetGroupedWorkTimeDTO(user.username, groupedWorkTimes.sortBy(_.date)).asJson
                }
              } yield result

            case Left(ex) =>
              BadRequest(ex.asJson)
          }
        }
    }
  }

  def endpoints(
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserServiceAlgebra[F],
    workService: WorkServiceAlgebra[F],
  ): HttpRoutes[F] = {
    val allRolesRoutes = Auth.allRolesHandler {
      logWorkEndpoint(workService)
    }(TSecAuthService.empty)
    val adminRolesRoutes = Auth.adminOnly {
      otherUserLogWorkEndpoint(userService, workService)
    }
    auth.liftService(allRolesRoutes <+> adminRolesRoutes)
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
  ): F[Response[F]] = {
    for {
      workingTime           <- workService.collectWorkTimeForUserBetweenDates(user, fromDate, toDate)
      obligatoryWorkingTime <- workService.collectObligatoryWorkTimeForUser(user, fromDate, toDate)
      workingSeconds           = workingTime.toSeconds
      obligatoryWorkingSeconds = obligatoryWorkingTime.toSeconds
    } yield GetWorkingTimeResultDTO(workingSeconds, obligatoryWorkingSeconds)
  }.value >>= {
    case Right(dto) => Ok(dto.asJson)
    case Left(ex)   => BadRequest(ex.asJson)
  }

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
}
