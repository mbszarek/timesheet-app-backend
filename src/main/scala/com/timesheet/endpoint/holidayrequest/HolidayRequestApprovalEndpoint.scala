package com.timesheet.endpoint.holidayrequest

import java.time.LocalDate

import cats.data._
import cats.effect._
import cats.implicits._
import com.timesheet.core.auth.Auth
import com.timesheet.core.service.holidayapproval.HolidayApprovalServiceAlgebra
import com.timesheet.core.service.holidayrequest.HolidayRequestServiceAlgebra
import com.timesheet.core.service.user.UserServiceAlgebra
import com.timesheet.core.validation.ValidationUtils.BasicError
import com.timesheet.core.validation.date.DateValidatorAlgebra
import com.timesheet.endpoint.AuthEndpoint
import com.timesheet.model.rest.holidayrequest.{ApproveHolidayRequestDTO, DenyHolidayRequestDTO}
import com.timesheet.model.user.{User, UserId}
import io.circe.generic.auto._
import io.circe.syntax._
import fs2._
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
    dateValidator: DateValidatorAlgebra[F],
  ): AuthEndpoint[F, Auth] = {
    case req @ POST -> Root / "approve" asAuthed user => {
      for {
        request <- EitherT
          .right[BasicError](req.request.as[ApproveHolidayRequestDTO])

        requestedUser <- userService
          .getUserByUsername(request.username)
          .leftWiden[BasicError]

        _ <- checkRequestDates(dateValidator, request.fromDate, request.toDate)

        holidayRequests <- holidayRequestService
          .getAllPendingHolidayRequestsForUserBetweenDates(requestedUser.id, request.fromDate, request.toDate)
          .leftWiden[BasicError]

        holidays <- EitherT
          .right[BasicError] {
            (for {
              holidayRequest <- Stream(holidayRequests: _*)
                .covary[F]

              holiday <- Stream
                .eval {
                  holidayApprovalService
                    .approveHolidays(user, holidayRequest)
                }
            } yield holiday)
              .compile
              .toList
          }
      } yield holidays
    }.value >>= {
      case Left(ex)        => BadRequest(ex.asJson)
      case Right(holidays) => Ok(holidays.asJson)
    }

    case req @ POST -> Root / "deny" asAuthed user => {
      for {
        request <- EitherT
          .right[BasicError](req.request.as[DenyHolidayRequestDTO])

        requestedUser <- userService
          .getUserByUsername(request.username)
          .leftWiden[BasicError]

        _ <- checkRequestDates(dateValidator, request.fromDate, request.toDate)

        holidayRequests <- holidayRequestService
          .getAllPendingHolidayRequestsForUserBetweenDates(requestedUser.id, request.fromDate, request.toDate)
          .leftWiden[BasicError]

        holidays <- EitherT
          .right[BasicError] {
            (for {
              holidayRequest <- Stream(holidayRequests: _*)
                .covary[F]

              holiday <- Stream
                .eval {
                  holidayApprovalService
                    .denyHolidays(user, request.reason, holidayRequest)
                }
            } yield holiday)
              .compile
              .toList
          }
      } yield holidays
    }.value >>= {
      case Left(ex)        => BadRequest(ex.asJson)
      case Right(holidays) => Ok(holidays.asJson)
    }
  }

  def endpoints(
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserServiceAlgebra[F],
    holidayRequestService: HolidayRequestServiceAlgebra[F],
    holidayApprovalService: HolidayApprovalServiceAlgebra[F],
    dateValidator: DateValidatorAlgebra[F],
  ): HttpRoutes[F] =
    auth.liftService {
      Auth.adminOnly {
        approveHolidayRequestEndpoint(
          userService,
          holidayRequestService,
          holidayApprovalService,
          dateValidator,
        )
      }
    }

  private def checkRequestDates(
    dateValidator: DateValidatorAlgebra[F],
    firstDate: LocalDate,
    nextDate: LocalDate,
  ): EitherT[F, BasicError, Unit] = {
    for {
      _ <- dateValidator
        .isDateInTheFuture(firstDate.atStartOfDay())

      _ <- dateValidator
        .areDatesInProperOrder(firstDate.atStartOfDay(), nextDate.atStartOfDay())
    } yield ()
  }.leftWiden[BasicError]
}

object HolidayRequestApprovalEndpoint {
  def endpoint[F[_]: Sync, Auth: JWTMacAlgo](
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserServiceAlgebra[F],
    holidayRequestService: HolidayRequestServiceAlgebra[F],
    holidayApprovalService: HolidayApprovalServiceAlgebra[F],
    dateValidator: DateValidatorAlgebra[F],
  ): HttpRoutes[F] =
    new HolidayRequestApprovalEndpoint[F, Auth]
      .endpoints(auth, userService, holidayRequestService, holidayApprovalService, dateValidator)
}
