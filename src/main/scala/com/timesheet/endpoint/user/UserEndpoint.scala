package com.timesheet.endpoint.user

import cats.data._
import cats.effect._
import cats.syntax.all._
import com.timesheet.core.auth.Auth
import com.timesheet.core.error.AuthenticationError
import com.timesheet.core.service.user.UserServiceAlgebra
import com.timesheet.core.validation.ValidationUtils._
import com.timesheet.endpoint.AuthEndpoint
import com.timesheet.model.login.{LoginRequest, SignupRequest}
import com.timesheet.model.user.{User, UserId}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import tsec.authentication._
import tsec.common.Verified
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.passwordhashers.{PasswordHash, PasswordHasher}

class UserEndpoint[F[_]: Sync, A, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  private def loginEndpoint(
    userService: UserServiceAlgebra[F],
    cryptService: PasswordHasher[F, A],
    auth: Authenticator[F, UserId, User, AugmentedJWT[Auth, UserId]],
  ): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "login" =>
        val action = for {
          login <- EitherT.liftF(req.as[LoginRequest])
          name = login.username
          user        <- userService.getUserByUsername(name)
          checkResult <- EitherT.liftF(cryptService.checkpw(login.password, PasswordHash[A](user.hash)))
          _ <- checkResult match {
            case Verified => EitherT.rightT[F, UserDoesNotExists.type](())
            case _        => EitherT.leftT[F, User](UserDoesNotExists)
          }
          token <- EitherT.right[UserDoesNotExists.type](auth.create(user.id))
        } yield (user, token)

        action.value >>= {
          case Right((user, token))    => Ok(user.asJson).map(auth.embed(_, token))
          case Left(UserDoesNotExists) => BadRequest(AuthenticationError("Authentication failed").asJson)
        }
    }

  private def signupEndpoint(
    userService: UserServiceAlgebra[F],
    cryptService: PasswordHasher[F, A],
  ): AuthEndpoint[F, Auth] = {
    case req @ POST -> Root asAuthed _ =>
      for {
        signup        <- req.request.as[SignupRequest]
        hash          <- cryptService.hashpw(signup.password)
        user          <- signup.asUser(hash).pure[F]
        serviceResult <- userService.create(user).value
        result <- serviceResult match {
          case Right(saved)                  => Ok(saved.asJson)
          case Left(UserAlreadyExists(user)) => Conflict(s"Cannot create user with username: ${user.username}")
        }
      } yield result
  }

  def getAllUsersEndpoint(
    userService: UserServiceAlgebra[F],
  ): AuthEndpoint[F, Auth] = {
    case GET -> Root asAuthed _ =>
      userService.getAll() >>= { users =>
        Ok(users.asJson)
      }
  }

  def getCurrentUserEndpoint(): AuthEndpoint[F, Auth] = {
    case GET -> Root / "me" asAuthed user =>
      Ok(user.asJson)
  }

  def endpoints(
    userService: UserServiceAlgebra[F],
    cryptService: PasswordHasher[F, A],
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
  ): HttpRoutes[F] = {
    val allRolesRoutes = Auth.allRolesHandler {
      getAllUsersEndpoint(userService) orElse
      getCurrentUserEndpoint()
    }(TSecAuthService.empty)
    val adminRoutes = Auth.adminOnly {
      signupEndpoint(userService, cryptService)
    }
    loginEndpoint(userService, cryptService, auth.authenticator) <+> auth.liftService(allRolesRoutes <+> adminRoutes)
  }
}

object UserEndpoint {
  def endpoint[F[_]: Sync, A, Auth: JWTMacAlgo](
    userService: UserServiceAlgebra[F],
    cryptService: PasswordHasher[F, A],
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
  ): HttpRoutes[F] = new UserEndpoint[F, A, Auth].endpoints(userService, cryptService, auth)
}
