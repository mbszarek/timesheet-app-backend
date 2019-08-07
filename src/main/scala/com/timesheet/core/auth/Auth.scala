package com.timesheet.core.auth

import cats._
import cats.implicits._
import cats.effect._
import com.timesheet.model.user.{Role, User}
import com.timesheet.model.user.User._
import org.http4s.Response
import tsec.authentication.{AugmentedJWT, BackingStore, IdentityStore, JWTAuthenticator, SecuredRequest, TSecAuthService}
import tsec.authorization.BasicRBAC
import tsec.common.SecureRandomId
import tsec.jws.mac.JWSMacCV
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.mac.jca.MacSigningKey

import scala.concurrent.duration._

object Auth {
  def jwtAuthenticator[F[_]: Sync, Auth: JWTMacAlgo](
    key: MacSigningKey[Auth],
    authStore: BackingStore[F, SecureRandomId, AugmentedJWT[Auth, UserId]],
    userStore: IdentityStore[F, UserId, User],
  )(implicit cv: JWSMacCV[F, Auth]): JWTAuthenticator[F, UserId, User, Auth] =
    JWTAuthenticator.backed.inBearerToken(
      expiryDuration = 1.hour,
      maxIdle = None,
      tokenStore = authStore,
      identityStore = userStore,
      signingKey = key,
    )

  private def allRolesHelper[F[_], Auth](implicit F: MonadError[F, Throwable]): BasicRBAC[F, Role, User, Auth] =
    BasicRBAC.all[F, Role, User, Auth]

  def allRoles[F[_], Auth](
    pf: PartialFunction[SecuredRequest[F, User, AugmentedJWT[Auth, UserId]], F[Response[F]]]
  )(implicit F: MonadError[F, Throwable]): TSecAuthService[User, AugmentedJWT[Auth, UserId], F] =
    TSecAuthService.withAuthorization(allRolesHelper[F, AugmentedJWT[Auth, UserId]])(pf)

  def allRolesHandler[F[_], Auth](
    pf: PartialFunction[SecuredRequest[F, User, AugmentedJWT[Auth, UserId]], F[Response[F]]]
  )(
    onNotAuthorized: TSecAuthService[User, AugmentedJWT[Auth, UserId], F]
  )(implicit F: MonadError[F, Throwable]): TSecAuthService[User, AugmentedJWT[Auth, UserId], F] =
    TSecAuthService.withAuthorizationHandler(allRolesHelper[F, AugmentedJWT[Auth, UserId]])(pf, onNotAuthorized.run)

  private def adminOnlyHelper[F[_], Auth](implicit F: MonadError[F, Throwable]): BasicRBAC[F, Role, User, Auth] =
    BasicRBAC[F, Role, User, Auth](Role.Admin)

  def adminOnly[F[_], Auth](
    pf: PartialFunction[SecuredRequest[F, User, AugmentedJWT[Auth, UserId]], F[Response[F]]]
  )(implicit F: MonadError[F, Throwable]): TSecAuthService[User, AugmentedJWT[Auth, UserId], F] =
    TSecAuthService.withAuthorization(adminOnlyHelper[F, AugmentedJWT[Auth, UserId]])(pf)
}
