package com.timesheet.endpoint.workreport

import java.util.concurrent.Executors

import cats.implicits._
import cats.effect.{Blocker, ContextShift, Sync}
import com.timesheet.core.service.workReport.WorkReportServiceAlgebra
import com.timesheet.EndpointUtils._
import com.timesheet.core.auth.Auth
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
      workReportService.createReport(user, fromDate, toDate) {
        case Left(ex) => BadRequest(ex.asJson)
        case Right(file) =>
          StaticFile
            .fromFile(file, blocker, Some(request.request))
            .getOrElseF(NotFound())
      }
  }

  def endpoints(
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    workReportService: WorkReportServiceAlgebra[F],
  )(implicit
    cs: ContextShift[F],
  ): HttpRoutes[F] = {
    val allRolesRoutes = Auth.allRolesHandler {
      userWorkReportEndpoint(workReportService)
    }(TSecAuthService.empty)
    auth.liftService(allRolesRoutes)
  }

}

object WorkReportEndpoint {
  def endpoint[F[_]: Sync, Auth: JWTMacAlgo](
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
    workReportService: WorkReportServiceAlgebra[F],
  )(implicit
    cs: ContextShift[F],
  ): HttpRoutes[F] = new WorkReportEndpoint[F, Auth].endpoints(auth, workReportService)

  private val blockingEc = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))
  val blocker: Blocker   = Blocker.liftExecutionContext(blockingEc)
}
