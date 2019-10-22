package com.timesheet
package core.store.auth

import cats.implicits._
import cats.data.OptionT
import com.timesheet.concurrent.FutureConcurrentEffect
import com.timesheet.core.db.MongoDriverMixin
import com.timesheet.model.user.User.UserId
import org.mongodb.scala.MongoCollection
import tsec.authentication.{AugmentedJWT, BackingStore}
import tsec.common.SecureRandomId
import tsec.jws.JWSSerializer
import tsec.jws.mac.{JWSMacCV, JWSMacHeader}
import tsec.mac.jca.{MacErrorM, MacSigningKey}
import org.mongodb.scala.model.Filters._

class AuthStoreMongo[F[_]: FutureConcurrentEffect, A](key: MacSigningKey[A])(
  implicit hs: JWSSerializer[JWSMacHeader[A]],
  s: JWSMacCV[MacErrorM, A],
) extends BackingStore[F, SecureRandomId, AugmentedJWT[A, UserId]]
    with MongoDriverMixin[F] {
  override type T = GenCodecJWT

  protected val collection: F[MongoCollection[GenCodecJWT]] = getCollection("auth")

  override def put(elem: AugmentedJWT[A, UserId]): F[AugmentedJWT[A, UserId]] =
    for {
      coll <- collection
      entity = GenCodecJWT.fromJWT(elem)
      _ <- coll.insertOne(entity).toFS2.drain
    } yield elem

  override def update(v: AugmentedJWT[A, UserId]): F[AugmentedJWT[A, UserId]] =
    for {
      coll <- collection
      entity = GenCodecJWT.fromJWT(v)
      _ <- coll.replaceOne(equal("id", entity.id), entity).toFS2.drain
    } yield v

  override def delete(id: SecureRandomId): F[Unit] =
    for {
      coll <- collection
      _    <- coll.deleteOne(equal("id", id)).toFS2.drain
    } yield ()

  override def get(id: SecureRandomId): OptionT[F, AugmentedJWT[A, UserId]] =
    OptionT {
      for {
        coll   <- collection
        entity <- coll.find(equal("id", id)).toFS2.last
      } yield entity.map(_.toJWT(id, key))
    }
}

object AuthStoreMongo {
  def apply[F[_]: FutureConcurrentEffect, A](
    key: MacSigningKey[A]
  )(implicit hs: JWSSerializer[JWSMacHeader[A]], s: JWSMacCV[MacErrorM, A]): AuthStoreMongo[F, A] =
    new AuthStoreMongo(key)
}
