package com.timesheet.core.service.init.config.entities

import com.timesheet.core.service.init.config.entities.InitConfig.InitUser
import com.timesheet.model.user.{Role, User, UserId}
import tsec.passwordhashers.PasswordHash

final case class InitConfig(
  users: List[InitUser],
)

object InitConfig {
  final private[init] case class InitUser(
    username: String,
    firstName: String,
    lastName: String,
    email: String,
    phone: String,
    password: String,
    role: Role,
  ) {
    def toUser[A](passwordHash: PasswordHash[A]): User = User(
      UserId.createNew(),
      username,
      firstName,
      lastName,
      email,
      passwordHash,
      phone,
      role,
    )
  }
}
