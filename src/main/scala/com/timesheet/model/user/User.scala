package com.timesheet.model.user

final case class User(
  id: Option[Long] = None,
  userName: String,
  firstName: String,
  lastName: String,
  email: String,
  hash: String,
  phone: String,
  role: Role,
)
