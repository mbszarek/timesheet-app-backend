package com.timesheet.endpoint.user

import cats.data._
import cats.effect._
import cats.implicits._
import com.timesheet.core.service.user.UserService
import com.timesheet.core.validation.ValidationUtils._
import com.timesheet.model.login.LoginRequest
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}
import tsec.authentication.{AugmentedJWT, Authenticator, SecuredRequestHandler}
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.passwordhashers.PasswordHasher

class UserEndpoint[F[_]: Sync, A, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  implicit val userDecoder: EntityDecoder[F, User]             = jsonOf
  implicit val loginReqDecoder: EntityDecoder[F, LoginRequest] = jsonOf

  private def loginEndpoint(
    userService: UserService[F],
    cryptService: PasswordHasher[F, A],
    auth: Authenticator[F, UserId, User, AugmentedJWT[Auth, UserId]]
  ): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "login" =>
        val action = for {
          login <- EitherT.liftF(req.as[LoginRequest])
          name = login.userName
          user        <- userService.getUserByUserName(name)
//          checkResult <- EitherT.liftF(cryptService.checkpw(login.password, PasswordHash[A](user.hash)))
          /*_ <- checkResult match {
            case Verified => EitherT.rightT[F, UserDoesNotExists.type](())
            case _        => EitherT.leftT[F, User](UserDoesNotExists)
          }*/
          token <- user.id match {
            case None     => throw new Exception("Impossibru")
            case Some(id) => EitherT.right[UserDoesNotExists.type](auth.create(id))
          }
        } yield (user, token)

        action.value.flatMap {
          case Right((user, token))    => Ok(user.asJson).map(auth.embed(_, token))
          case Left(UserDoesNotExists) => BadRequest("Authentication failed")
        }
    }

  def endpoints(
    userService: UserService[F],
    cryptService: PasswordHasher[F, A],
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
  ): HttpRoutes[F] = loginEndpoint(userService, cryptService, auth.authenticator)
}

object UserEndpoint {
  def endpoint[F[_]: Sync, A, Auth: JWTMacAlgo](
    userService: UserService[F],
    cryptService: PasswordHasher[F, A],
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
  ): HttpRoutes[F] = new UserEndpoint[F, A, Auth].endpoints(userService, cryptService, auth)
}
