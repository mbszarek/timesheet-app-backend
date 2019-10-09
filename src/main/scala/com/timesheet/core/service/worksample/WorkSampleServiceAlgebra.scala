package com.timesheet.core.service.worksample

import com.timesheet.model.user.User
import cats.data._
import com.timesheet.core.validation.ValidationUtils.WorkSampleValidationError
import com.timesheet.model.worksample.WorkSample

trait WorkSampleServiceAlgebra[F[_]] {
  def tagWorkerEntrance(user: User): EitherT[F, WorkSampleValidationError, WorkSample]

  def tagWorkerExit(user: User): EitherT[F, WorkSampleValidationError, WorkSample]
}
