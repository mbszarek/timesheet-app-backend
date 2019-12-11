package com.timesheet.endpoint.holidayrequest

import cats.data.EitherT
import cats.effect._
import cats.implicits._
import com.timesheet.EndpointUtils._
import com.timesheet.core.auth.Auth
import com.timesheet.service.holidayrequest.HolidayRequestServiceAlgebra
import com.timesheet.core.error.ValidationErrors.{BasicError, DateValidationError}
import com.timesheet.endpoint.AuthEndpoint
import com.timesheet.model.db.ID
import com.timesheet.model.rest.holidayrequest.CreateHolidayRequestDTO
import com.timesheet.model.user.{User, UserId}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

final class HolidayRequestEndpoint[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  private def getHolidayRequestsEndpointForUser(
    holidayRequestService: HolidayRequestServiceAlgebra[F],
  ): AuthEndpoint[F, Auth] = {
    case GET -> Root asAuthed user =>
      for {
        holidayRequests <- holidayRequestService
          .getAllHolidayRequestsForUser(user.id)
        response <- Ok(holidayRequests.asJson)
      } yield response

    case GET -> Root / "betweenDates" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(toDate) asAuthed user =>
      holidayRequestService
        .getAllHolidayRequestsForUserBetweenDates(user.id, fromDate, toDate)
        .value >>= {
        case Right(holidayRequests) => Ok(holidayRequests.asJson)
        case Left(ex)               => BadRequest(ex.asJson)
      }
  }

  private def getHolidayRequestsEndpointForAdmin(
    holidayRequestService: HolidayRequestServiceAlgebra[F],
  ): AuthEndpoint[F, Auth] = {
    case GET -> Root / "all" asAuthed _ =>
      for {
        holidayRequests <- holidayRequestService
          .getAllHolidayRequests()
        response <- Ok(holidayRequests.asJson)
      } yield response

    case GET -> Root / "all" / "betweenDates" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(toDate) asAuthed _ =>
      holidayRequestService
        .getAllHolidayRequestsForSpecifiedDateRange(fromDate, toDate)
        .value >>= {
        case Right(holidayRequests) => Ok(holidayRequests.asJson)
        case Left(ex)               => BadRequest(ex.asJson)
      }
  }

  private def createHolidayRequestsEndpointForUser(
    holidayRequestService: HolidayRequestServiceAlgebra[F],
  ): AuthEndpoint[F, Auth] = {
    case req @ POST -> Root asAuthed user => {
      for {
        request <- EitherT
          .right[DateValidationError](req.request.as[CreateHolidayRequestDTO])

        _ <- holidayRequestService.createHolidayRequest(
          user,
          request.fromDate,
          request.toDate,
          request.holidayType,
          request.description,
        )
      } yield ()
    }.value >>= {
      case Right(_) => Created()
      case Left(ex) => BadRequest(ex.asJson)
    }

    case DELETE -> Root / id asAuthed user => {
      for {
        id <- EitherT
          .rightT[F, BasicError](ID(id))

        holidayRequests <- holidayRequestService.deleteHolidayRequest(
          user,
          id,
        )
      } yield holidayRequests
    }.value >>= {
      case Right(holidayRequest) => Ok(holidayRequest.asJson)
      case Left(ex)              => BadRequest(ex.asJson)
    }
  }

  def endpoints(
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    holidayRequestService: HolidayRequestServiceAlgebra[F],
  ): HttpRoutes[F] = {
    val allRolesRoutes = Auth.allRolesHandler {
      getHolidayRequestsEndpointForUser(holidayRequestService) orElse
      createHolidayRequestsEndpointForUser(holidayRequestService)
    }(TSecAuthService.empty)
    val adminOnlyRoutes = Auth.adminOnly {
      getHolidayRequestsEndpointForAdmin(holidayRequestService)
    }
    auth.liftService(allRolesRoutes <+> adminOnlyRoutes)
  }
}

object HolidayRequestEndpoint {
  def endpoint[F[_]: Sync, Auth: JWTMacAlgo](
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    holidayRequestService: HolidayRequestServiceAlgebra[F],
  ): HttpRoutes[F] = new HolidayRequestEndpoint[F, Auth].endpoints(auth, holidayRequestService)
}
