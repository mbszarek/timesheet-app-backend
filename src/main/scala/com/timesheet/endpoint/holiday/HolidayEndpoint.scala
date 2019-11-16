package com.timesheet.endpoint.holiday

import cats.implicits._
import cats.effect._
import com.timesheet.core.service.holiday.HolidayServiceAlgebra
import com.timesheet.EndpointUtils._
import com.timesheet.core.auth.Auth
import com.timesheet.endpoint.AuthEndpoint
import com.timesheet.model.user.{User, UserId}
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder._
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

final class HolidayEndpoint[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  private def getHolidayEndpoint(holidayService: HolidayServiceAlgebra[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root asAuthed user =>
      for {
        holidays <- holidayService.collectHolidaysForUser(user.id)
        result   <- Ok(holidays.asJson)
      } yield result

    case GET -> Root / "betweenDates" :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(toDate) asAuthed user =>
      holidayService
        .collectHolidaysForUserBetweenDates(user.id, fromDate, toDate)
        .value >>= {
        case Right(holidays) => Ok(holidays.asJson)
        case Left(ex)        => BadRequest(ex.asJson)
      }
  }

  def endpoints(
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    holidayService: HolidayServiceAlgebra[F],
  ): HttpRoutes[F] = {
    val allRolesRoutes = Auth.allRolesHandler {
      getHolidayEndpoint(holidayService)
    }(TSecAuthService.empty)
    auth.liftService(allRolesRoutes)
  }

}

object HolidayEndpoint {
  def endpoint[F[_]: Sync, Auth: JWTMacAlgo](
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    holidayService: HolidayServiceAlgebra[F],
  ): HttpRoutes[F] = new HolidayEndpoint[F, Auth].endpoints(auth, holidayService)
}
