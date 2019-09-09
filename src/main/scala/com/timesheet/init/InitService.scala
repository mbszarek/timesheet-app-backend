package com.timesheet.init

import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.effect.ConcurrentEffect
import com.timesheet.core.service.user.UserService
import com.timesheet.model.user.{Role, User}
import tsec.passwordhashers.PasswordHasher

class InitService[F[_]: ConcurrentEffect, A](
  passwordHasher: PasswordHasher[F, A],
  userService: UserService[F],
) {
  import InitService.Admin

  def init: F[Unit] =
    for {
      adminPwHash <- passwordHasher.hashpw(Admin.hash)
      _           <- userService.create(Admin.copy(hash = adminPwHash.toString)).value
    } yield ()
}

object InitService {
  def apply[F[_]: ConcurrentEffect, A](
    passwordHasher: PasswordHasher[F, A],
    userService: UserService[F]
  ): InitService[F, A] =
    new InitService(passwordHasher, userService)

  private val Admin = User(
    Option.empty,
    "avsystem",
    "AV",
    "System",
    "kontakt@avsystem.com",
    "homarulek",
    "725482699",
    Role.Admin,
  )
}
