package com.timesheet.init

import cats.effect.ConcurrentEffect
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.timesheet.core.service.user.impl.UserService
import com.timesheet.model.user.User.UserId
import com.timesheet.model.user.{Role, User}
import tsec.passwordhashers.PasswordHasher

class InitService[F[_]: ConcurrentEffect, A](
  passwordHasher: PasswordHasher[F, A],
  userService: UserService[F],
) {
  import InitService.{Admin, NonAdmin}

  def init: F[Unit] =
    for {
      _ <- insertAccount(Admin)
      _ <- insertAccount(NonAdmin)
    } yield ()

  private def insertAccount(user: User): F[Unit] =
    for {
      pwHash <- passwordHasher.hashpw(user.hash)
      _      <- userService.create(user.copy(hash = pwHash)).value
    } yield ()
}

object InitService {
  def apply[F[_]: ConcurrentEffect, A](
    passwordHasher: PasswordHasher[F, A],
    userService: UserService[F]
  ): InitService[F, A] =
    new InitService(passwordHasher, userService)

  private val Admin = User(
    UserId.createNew(),
    "avsystem",
    "AV",
    "System",
    "kontakt@avsystem.com",
    "homarulek",
    "725482699",
    Role.Admin,
  )

  private val NonAdmin = User(
    UserId.createNew(),
    "mszarek",
    "Mateusz",
    "Szarek",
    "mszarek@avsystem.com",
    "homarulek",
    "725482699",
    Role.Customer,
  )
}
