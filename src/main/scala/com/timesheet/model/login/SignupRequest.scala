package com.timesheet.model.login

import com.timesheet.model.user.{Role, User}
import com.timesheet.model.user.User.UserId
import tsec.passwordhashers.PasswordHash

final case class SignupRequest(
  username: String,
  firstName: String,
  lastName: String,
  email: String,
  password: String,
  phone: String,
  role: Role,
) {
  def asUser[A](hashedPassword: PasswordHash[A]): User = User(
    UserId.createNew(),
    username,
    firstName,
    lastName,
    email,
    hashedPassword.toString,
    phone,
    role = role,
    isCurrentlyAtWork = None,
  )
}
