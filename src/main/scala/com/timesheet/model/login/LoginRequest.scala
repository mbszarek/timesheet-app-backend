package com.timesheet.model.login

import com.timesheet.model.user.{Role, User}
import tsec.passwordhashers.PasswordHash

final case class LoginRequest(
  username: String,
  password: String,
)

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
    Option.empty,
    username,
    firstName,
    lastName,
    email,
    hashedPassword.toString,
    phone,
    role = role,
  )
}
