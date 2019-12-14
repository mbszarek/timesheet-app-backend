package com.timesheet.endpoint.workreport

import java.time.LocalDate
import java.util.concurrent.Executors

import cats.implicits._
import cats.effect.{Blocker, ContextShift, Sync}
import com.timesheet.service.workReport.WorkReportServiceAlgebra
import com.timesheet.EndpointUtils._
import com.timesheet.core.auth.Auth
import com.timesheet.service.user.UserServiceAlgebra
import com.timesheet.endpoint.AuthEndpoint
import com.timesheet.model.user.{User, UserId}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.authentication._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s._

import scala.concurrent.ExecutionContext

final class WorkReportEndpoint[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  import WorkReportEndpoint.blocker

  private def userWorkReportEndpoint(
    workReportService: WorkReportServiceAlgebra[F],
  )(implicit
    cs: ContextShift[F],
  ): AuthEndpoint[F, Auth] = {
    case request @ GET -> Root :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(toDate) asAuthed user =>
      getReport(
        workReportService,
        user,
        fromDate,
        toDate,
        request.request,
      )
  }

  private def adminWorkReportEndpoint(
    userService: UserServiceAlgebra[F],
    workReportService: WorkReportServiceAlgebra[F],
  )(implicit
    cs: ContextShift[F],
  ): AuthEndpoint[F, Auth] = {
    def withOtherUser(username: String)(fun: User => F[Response[F]]): F[Response[F]] =
      userService
        .getUserByUsername(username)
        .value >>= {
        case Right(user) => fun(user)
        case Left(_)     => NotFound()
      }

    {
      case request @ GET -> Root / username :? FromLocalDateMatcher(fromDate) +& ToLocalDateMatcher(toDate) asAuthed _ =>
        withOtherUser(username) { user =>
          getReport(
            workReportService,
            user,
            fromDate,
            toDate,
            request.request,
          )
        }
    }
  }

  private def getReport(
    workReportService: WorkReportServiceAlgebra[F],
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
    request: Request[F],
  )(implicit
    cs: ContextShift[F],
  ): F[Response[F]] =
    workReportService.createReport(user, fromDate, toDate) {
      case Left(ex) => BadRequest(ex.asJson)
      case Right(file) =>
        StaticFile
          .fromFile(
            file,
            blocker,
            Some(request),
          )
          .map { response =>
            response.withHeaders(
              response.headers ++ Headers
                .of(Header("Content-Disposition", """attachment; filename="report.txt"""")),
            )
          }
          .getOrElseF(NotFound())
    }

  def endpoints(
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserServiceAlgebra[F],
    workReportService: WorkReportServiceAlgebra[F],
  )(implicit
    cs: ContextShift[F],
  ): HttpRoutes[F] = {
    val allRolesRoutes = Auth.allRolesHandler {
      userWorkReportEndpoint(workReportService)
    }(TSecAuthService.empty)
    val adminRoutes = Auth.employerAdminOnly {
      adminWorkReportEndpoint(userService, workReportService)
    }
    auth.liftService(allRolesRoutes <+> adminRoutes)
  }

}

object WorkReportEndpoint {
  def endpoint[F[_]: Sync, Auth: JWTMacAlgo](
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    userService: UserServiceAlgebra[F],
    workReportService: WorkReportServiceAlgebra[F],
  )(implicit
    cs: ContextShift[F],
  ): HttpRoutes[F] = new WorkReportEndpoint[F, Auth].endpoints(auth, userService, workReportService)

  private val blockingEc = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))
  val blocker: Blocker   = Blocker.liftExecutionContext(blockingEc)
}
