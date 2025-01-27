package com.timesheet.core.validation.worksample

import cats.data._
import com.timesheet.model.user.User
import com.timesheet.model.work.ActivityType

trait WorkSampleValidatorAlgebra[F[_]] {
  import com.timesheet.core.error.ValidationErrors.WorkSampleValidationError

  def hasUserCorrectState(
    user: User,
    activityType: ActivityType,
  ): EitherT[F, WorkSampleValidationError, Unit]
}
