package com.timesheet.concurrent

import cats.effect.ConcurrentEffect

import scala.concurrent.{ExecutionContext, Future}

trait FutureConcurrentEffect[F[_]] extends ConcurrentEffect[F] {
  def wrapFuture[A](future: ExecutionContext => Future[A]): F[A]
}

object FutureConcurrentEffect {
  def apply[F[_]](implicit F: FutureConcurrentEffect[F]): FutureConcurrentEffect[F] = F
}
