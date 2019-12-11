package com.timesheet.endpoint.holidayrequest

import cats.data._
import cats.effect._
import cats.implicits._
import com.timesheet.core.auth.Auth
import com.timesheet.service.holidayapproval.HolidayApprovalServiceAlgebra
import com.timesheet.service.holidayrequest.HolidayRequestServiceAlgebra
import com.timesheet.service.user.UserServiceAlgebra
import com.timesheet.core.error.ValidationErrors.{BasicError, EntityNotFound}
import com.timesheet.core.validation.date.DateValidatorAlgebra
import com.timesheet.core.validation.user.UserValidatorAlgebra
import com.timesheet.endpoint.AuthEndpoint
import com.timesheet.model.db.ID
import com.timesheet.model.holidayrequest.HolidayRequest
import com.timesheet.model.user.{User, UserId}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

final class HolidayRequestApprovalEndpoint[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  private def approveHolidayRequestEndpoint(
    userService: UserServiceAlgebra[F],
    holidayRequestService: HolidayRequestServiceAlgebra[F],
    holidayApprovalService: HolidayApprovalServiceAlgebra[F],
    userValidator: UserValidatorAlgebra[F],
    dateValidator: DateValidatorAlgebra[F],
  ): AuthEndpoint[F, Auth] = {
    case POST -> Root / id / "approve" asAuthed user => {
      for {
        id <- EitherT
          .rightT[F, BasicError](ID(id))
        holidayRequest <- holidayRequestService.get(id)
        _              <- userValidator.canModifyResource(user, holidayRequest)
        holiday <- EitherT.right[BasicError] {
          holidayApprovalService.approveHolidays(user, holidayRequest)
        }
      } yield holiday
    }.value >>= {
      case Left(ex)       => BadRequest(ex.asJson)
      case Right(holiday) => Ok(holiday.asJson)
    }

    case POST -> Root / id / "deny" asAuthed user => {
      for {
        id <- EitherT
          .rightT[F, BasicError](ID(id))
        holidayRequest <- holidayRequestService.get(id)
        _              <- userValidator.canModifyResource(user, holidayRequest)
        holidayOpt <- EitherT.right[BasicError] {
          holidayApprovalService.denyHolidays(user, None, holidayRequest)
        }
        holiday <- holidayOpt.fold[EitherT[F, BasicError, HolidayRequest]] {
          EitherT.leftT[F, HolidayRequest](EntityNotFound(id))
        } { holiday =>
          EitherT.rightT[F, BasicError](holiday)
        }
      } yield holiday
    }.value >>= {
      case Left(ex)       => BadRequest(ex.asJson)
      case Right(holiday) => Ok(holiday.asJson)
    }
  }

  def endpoints(
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserServiceAlgebra[F],
    holidayRequestService: HolidayRequestServiceAlgebra[F],
    holidayApprovalService: HolidayApprovalServiceAlgebra[F],
    userValidator: UserValidatorAlgebra[F],
    dateValidator: DateValidatorAlgebra[F],
  ): HttpRoutes[F] =
    auth.liftService {
      Auth.adminOnly {
        approveHolidayRequestEndpoint(
          userService,
          holidayRequestService,
          holidayApprovalService,
          userValidator,
          dateValidator,
        )
      }
    }
}

object HolidayRequestApprovalEndpoint {
  def endpoint[F[_]: Sync, Auth: JWTMacAlgo](
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserServiceAlgebra[F],
    holidayRequestService: HolidayRequestServiceAlgebra[F],
    holidayApprovalService: HolidayApprovalServiceAlgebra[F],
    userValidator: UserValidatorAlgebra[F],
    dateValidator: DateValidatorAlgebra[F],
  ): HttpRoutes[F] =
    new HolidayRequestApprovalEndpoint[F, Auth]
      .endpoints(auth, userService, holidayRequestService, holidayApprovalService, userValidator, dateValidator)
}
