package com.timesheet.endpoint.user

import cats.data._
import cats.effect._
import cats.implicits._
import com.timesheet.core.auth.Auth
import com.timesheet.core.service.user.UserService
import com.timesheet.core.validation.ValidationUtils._
import com.timesheet.endpoint.AuthEndpoint
import com.timesheet.model.login.{LoginRequest, SignupRequest}
import com.timesheet.model.user.{AuthenticationError, User}
import com.timesheet.model.user.User.UserId
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.passwordhashers.{PasswordHash, PasswordHasher}
import tsec.authentication._
import tsec.common.Verified

class UserEndpoint[F[_]: Sync, A, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  implicit val userDecoder: EntityDecoder[F, User]               = jsonOf
  implicit val loginReqDecoder: EntityDecoder[F, LoginRequest]   = jsonOf
  implicit val signupReqDecoder: EntityDecoder[F, SignupRequest] = jsonOf

  private def loginEndpoint(
    userService: UserService[F],
    cryptService: PasswordHasher[F, A],
    auth: Authenticator[F, UserId, User, AugmentedJWT[Auth, UserId]]
  ): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "login" =>
        val action = for {
          login <- EitherT.liftF(req.as[LoginRequest])
          name = login.username
          user        <- userService.getUserByUserName(name)
          checkResult <- EitherT.liftF(cryptService.checkpw(login.password, PasswordHash[A](user.hash)))
          _ <- checkResult match {
            case Verified => EitherT.rightT[F, UserDoesNotExists.type](())
            case _        => EitherT.leftT[F, User](UserDoesNotExists)
          }
          token <- user.id match {
            case None     => throw new Exception("Impossibru")
            case Some(id) => EitherT.right[UserDoesNotExists.type](auth.create(id))
          }
        } yield (user, token)

        action.value.flatMap {
          case Right((user, token))    => Ok(user.asJson).map(auth.embed(_, token))
          case Left(UserDoesNotExists) => BadRequest(AuthenticationError("Authentication failed").asJson)
        }
    }

  private def signupEndpoint(
    userService: UserService[F],
    cryptService: PasswordHasher[F, A],
  ): AuthEndpoint[F, Auth] = {
    case req @ POST -> Root asAuthed _ =>
      val action = for {
        signup <- req.request.as[SignupRequest]
        hash   <- cryptService.hashpw(signup.password)
        user   <- signup.asUser(hash).pure[F]
        result <- userService.create(user).value
      } yield result

      action.flatMap {
        case Right(saved)                  => Ok(saved.asJson)
        case Left(UserAlreadyExists(user)) => Conflict(s"Cannot create user with username: ${user.username}")
      }
  }

  def getAllUsersEndpoint(
    userService: UserService[F],
  ): AuthEndpoint[F, Auth] = {
    case GET -> Root asAuthed _ =>
      Ok(userService.getAll().asJson)
  }

  def endpoints(
    userService: UserService[F],
    cryptService: PasswordHasher[F, A],
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
  ): HttpRoutes[F] = {
    val adminRoutes = Auth.adminOnly {
      signupEndpoint(userService, cryptService)
    }
//    val getAllEndpoint = getAllUsersEndpoint(userService)
    loginEndpoint(userService, cryptService, auth.authenticator) <+> auth.liftService(adminRoutes)
  }
}

object UserEndpoint {
  def endpoint[F[_]: Sync, A, Auth: JWTMacAlgo](
    userService: UserService[F],
    cryptService: PasswordHasher[F, A],
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
  ): HttpRoutes[F] = new UserEndpoint[F, A, Auth].endpoints(userService, cryptService, auth)
}
