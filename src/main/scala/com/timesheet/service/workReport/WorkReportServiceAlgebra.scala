package com.timesheet.service.workReport

import java.io.File
import java.time.LocalDate

import com.timesheet.core.error.ValidationErrors.DateValidationError
import com.timesheet.model.user.User

trait WorkReportServiceAlgebra[F[_]] {
  def createReport[T](
    user: User,
    from: LocalDate,
    to: LocalDate,
  )(
    action: Either[DateValidationError, File] => F[T],
  ): F[T]
}
