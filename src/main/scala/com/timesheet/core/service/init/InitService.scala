package com.timesheet.core.service.init

import cats.effect._
import cats.implicits._
import com.timesheet.core.service.init.config.entities.InitConfig.InitUser
import com.timesheet.core.service.init.config.ConfigLoader
import com.timesheet.core.service.user.impl.UserService
import tsec.passwordhashers.PasswordHasher

final class InitService[F[_]: ConcurrentEffect, A](
  passwordHasher: PasswordHasher[F, A],
  userService: UserService[F],
  configLoader: ConfigLoader[F],
) {
  def init: fs2.Stream[F, Unit] =
    fs2.Stream.eval {
      for {
        initConfig <- configLoader.loadInitConfig()
        _          <- initConfig.users.traverse(insertAccount)
      } yield ()
    }

  private def insertAccount(initUser: InitUser): F[Unit] =
    for {
      pwHash <- passwordHasher.hashpw(initUser.password)
      _      <- userService.create(initUser.toUser(pwHash)).value
    } yield ()
}

object InitService {
  def apply[F[_]: ConcurrentEffect, A](
    passwordHasher: PasswordHasher[F, A],
    userService: UserService[F],
    configLoader: ConfigLoader[F],
  ): InitService[F, A] =
    new InitService(passwordHasher, userService, configLoader)
}
