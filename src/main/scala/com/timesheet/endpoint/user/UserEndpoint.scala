package com.timesheet.endpoint.user

import cats.data._
import cats.effect._
import cats.implicits._
import com.timesheet.core.auth.Auth
import com.timesheet.core.error.AuthenticationError
import com.timesheet.core.service.user.UserServiceAlgebra
import com.timesheet.core.validation.ValidationUtils._
import com.timesheet.endpoint.AuthEndpoint
import com.timesheet.model.rest.users.{LoginDTO, SignupDTO, UpdateUserDTO, UserDTO}
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

final class UserEndpoint[F[_]: Sync, A, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  private def loginEndpoint(
    userService: UserServiceAlgebra[F],
    cryptService: PasswordHasher[F, A],
    auth: Authenticator[F, UserId, User, AugmentedJWT[Auth, UserId]],
  ): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "login" => {
        for {
          login <- EitherT
            .right[UserValidationError](req.as[LoginDTO])

          name = login.username

          user <- userService
            .getUserByUsername(name)

          checkResult <- EitherT
            .right[UserValidationError](cryptService.checkpw(login.password, PasswordHash[A](user.hash)))

          _ <- checkResult match {
            case Verified => EitherT.rightT[F, UserValidationError](())
            case _        => EitherT.leftT[F, Unit](UserDoesNotExists).leftWiden[UserValidationError]
          }

          token <- EitherT
            .right[UserValidationError](auth.create(user.id))
        } yield (user, token)
      }.value >>= {
        case Right((user, token)) =>
          Ok {
            UserDTO
              .fromUser(user)
              .asJson
          }.map(auth.embed(_, token))
        case Left(_) => BadRequest(AuthenticationError("Authentication failed").asJson)
      }
    }

  private def signupEndpoint(
    userService: UserServiceAlgebra[F],
    cryptService: PasswordHasher[F, A],
  ): AuthEndpoint[F, Auth] = {
    case req @ POST -> Root asAuthed _ => {
      for {
        signup <- EitherT
          .right[UserValidationError](req.request.as[SignupDTO])

        hash <- EitherT
          .right[UserValidationError](cryptService.hashpw(signup.password))

        user <- EitherT
          .rightT[F, UserValidationError](signup.asUser(hash))

        result <- userService
          .create(user)
      } yield result
    }.value >>= {
      case Right(user) =>
        Ok {
          UserDTO
            .fromUser(user)
            .asJson
        }

      case Left(_) =>
        Conflict("Cannot create user")
    }
  }

  def getAllUsersEndpoint(userService: UserServiceAlgebra[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root asAuthed _ =>
      Nested(userService.getAll())
        .map(UserDTO.fromUser)
        .value >>= (users => Ok(users.asJson))
  }

  def getCurrentUserEndpoint(): AuthEndpoint[F, Auth] = {
    case GET -> Root / "me" asAuthed user =>
      Ok {
        UserDTO
          .fromUser(user)
          .asJson
      }
  }

  def otherUsersEndpoint(userService: UserServiceAlgebra[F]): AuthEndpoint[F, Auth] = {
    case GET -> Root / "info" / userName asAuthed _ =>
      userService
        .getUserByUsername(userName)
        .value >>= {
        case Right(user) =>
          Ok {
            UserDTO
              .fromUser(user)
              .asJson
          }

        case Left(_) =>
          NotFound()
      }

    case req @ PUT -> Root / "info" / userName asAuthed _ =>
      (for {
        user <- userService
          .getUserByUsername(userName)

        request <- EitherT
          .right[UserValidationError](req.request.as[UpdateUserDTO])

        result <- userService
          .update(request.updateUser(user))
      } yield result).value >>= {
        case Right(user) =>
          Ok {
            UserDTO
              .fromUser(user)
              .asJson
          }

        case Left(_) => Forbidden()
      }
  }

  def endpoints(
    userService: UserServiceAlgebra[F],
    cryptService: PasswordHasher[F, A],
    auth: SecuredRequestHandler[F, UserId, User, AugmentedJWT[Auth, UserId]],
  ): HttpRoutes[F] = {
    val allRolesRoutes = Auth.allRolesHandler {
      getAllUsersEndpoint(userService) orElse
      getCurrentUserEndpoint() orElse
      otherUsersEndpoint(userService)
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
