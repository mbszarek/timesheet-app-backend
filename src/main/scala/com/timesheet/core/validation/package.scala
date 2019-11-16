package com.timesheet.core

import cats.Applicative
import cats.data.EitherT

package object validation {
  implicit def eitherTOps(eitherT: EitherT.type): EitherTOps.type = EitherTOps

  object EitherTOps {
    def condUnit[F[_]: Applicative, A](
      cond: => Boolean,
      ifFalse: => A,
    ): EitherT[F, A, Unit] =
      EitherT.cond[F](
        cond,
        (),
        ifFalse,
      )
  }
}
