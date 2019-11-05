package com.timesheet.core.store.auth

import cats._
import cats.data.OptionT
import cats.implicits._
import com.timesheet.model.user.UserId
import tsec.authentication.{AugmentedJWT, BackingStore}
import tsec.common.SecureRandomId

import scala.collection.concurrent.TrieMap

final class AuthStoreInMemory[F[_]: Applicative, A] extends BackingStore[F, SecureRandomId, AugmentedJWT[A, UserId]] {
  private val cache = new TrieMap[SecureRandomId, AugmentedJWT[A, UserId]]

  override def put(jwt: AugmentedJWT[A, UserId]): F[AugmentedJWT[A, UserId]] = {
    cache += (jwt.id -> jwt)
    jwt.pure[F]
  }

  override def update(jwt: AugmentedJWT[A, UserId]): F[AugmentedJWT[A, UserId]] = {
    cache.update(jwt.id, jwt)
    jwt.pure[F]
  }

  override def delete(id: SecureRandomId): F[Unit] = {
    cache.remove(id)
    ().pure[F]
  }

  override def get(id: SecureRandomId): OptionT[F, AugmentedJWT[A, UserId]] = OptionT.fromOption {
    cache.get(id)
  }
}

object AuthStoreInMemory {
  def apply[F[_]: Applicative, A] = new AuthStoreInMemory[F, A]
}
