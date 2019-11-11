package com.timesheet.core.error

final case class AuthenticationError(message: String) extends RuntimeException
