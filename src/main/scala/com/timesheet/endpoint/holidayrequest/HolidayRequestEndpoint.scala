package com.timesheet.endpoint.holidayrequest

import cats.data.EitherT
import cats.effect._
import cats.implicits._
import com.timesheet.EndpointUtils._
import com.timesheet.core.auth.Auth
import com.timesheet.core.service.holidayrequest.HolidayRequestServiceAlgebra
import com.timesheet.endpoint.AuthEndpoint
import com.timesheet.model.rest.holidayrequest.HolidayRESTRequest
import com.timesheet.model.user.{User, UserId}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.circe.CirceEntityEncoder._
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
          .collectHolidayRequestsForUser(user.id)
        response <- Ok(holidayRequests.asJson)
      } yield response

    case GET -> Root / "betweenDates" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(toDate) asAuthed user =>
      for {
        holidayRequests <- holidayRequestService
          .collectHolidayRequestsForUserBetweenDates(user.id, fromDate, toDate)
        response <- Ok(holidayRequests.asJson)
      } yield response
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
      for {
        holidayRequests <- holidayRequestService
          .getAllHolidayRequestsForSpecifiedDateRange(fromDate, toDate)
        response <- Ok(holidayRequests.asJson)
      } yield response
  }

  private def createHolidayRequestsEndpointForAdmin(
    holidayRequestService: HolidayRequestServiceAlgebra[F],
  ): AuthEndpoint[F, Auth] = {
    case req @ POST -> Root asAuthed user =>
      for {
        request <- EitherT.liftF(req.request.as[HolidayRESTRequest])
      _ <- holidayRequestService.createHolidayRequestForDateRange(user, request.fromDate, request.toDate, request.holidayType, request.description)
      } yield ()
  }

  def endpoints(
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    holidayRequestService: HolidayRequestServiceAlgebra[F],
  ): HttpRoutes[F] = {
    val allRolesRoutes = Auth.allRolesHandler {
      getHolidayRequestsEndpointForUser(holidayRequestService)
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
