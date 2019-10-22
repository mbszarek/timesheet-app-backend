package com.timesheet.core.store

import java.time.Instant

import com.avsystem.commons.serialization.{HasGenCodecWithDeps, transientDefault}
import com.timesheet.model.user.UserId
import tsec.authentication.AugmentedJWT
import tsec.common.SecureRandomId
import tsec.jws.JWSSerializer
import tsec.jws.mac.{JWSMacCV, JWSMacHeader, JWTMacImpure}
import tsec.mac.jca.{MacErrorM, MacSigningKey}
import com.timesheet.util.InstantTypeClassInstances

package object auth {
  final case class GenCodecJWT(
    id: String,
    jwt: String,
    identity: UserId,
    expiry: Instant,
    @transientDefault lastTouched: Option[Instant] = None,
  ) {
    def toJWT[A](id: SecureRandomId, key: MacSigningKey[A])(
      implicit s: JWSMacCV[MacErrorM, A]
    ): AugmentedJWT[A, UserId] =
      JWTMacImpure.verifyAndParse(jwt, key) match {
        case Left(err) => throw new Exception(err)
        case Right(jwt) =>
          AugmentedJWT(id, jwt, identity, expiry, lastTouched)
      }
  }

  object GenCodecJWT extends HasGenCodecWithDeps[InstantTypeClassInstances.type, GenCodecJWT] {
    def fromJWT[A](jwt: AugmentedJWT[A, UserId])(implicit hs: JWSSerializer[JWSMacHeader[A]]): GenCodecJWT =
      GenCodecJWT(
        jwt.id,
        jwt.jwt.toEncodedString,
        jwt.identity,
        jwt.expiry,
        jwt.lastTouched,
      )
  }

}
