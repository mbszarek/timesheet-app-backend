package com.timesheet.core.validation.worksample.impl

import cats._
import cats.data._
import com.timesheet.core.error.ValidationErrors.{WorkSampleValidationError, WrongUserState}
import com.timesheet.core.validation.worksample.WorkSampleValidatorAlgebra
import com.timesheet.model.user.User
import com.timesheet.model.work._

final class WorkSampleValidator[F[_]: Applicative] extends WorkSampleValidatorAlgebra[F] {
  def hasUserCorrectState(
    user: User,
    activityType: ActivityType,
  ): EitherT[F, WorkSampleValidationError, Unit] = {
    lazy val wrongEither: EitherT[F, WorkSampleValidationError, Unit] = EitherT.leftT[F, Unit](WrongUserState)
    lazy val properEither: EitherT[F, WorkSampleValidationError, Unit] =
      EitherT.rightT[F, WorkSampleValidationError](())

    activityType match {
      case Entrance =>
        if (user.isCurrentlyAtWork) wrongEither
        else
          properEither

      case Departure =>
        if (user.isCurrentlyAtWork) properEither
        else
          wrongEither
    }
  }
}

object WorkSampleValidator {
  def apply[F[_]: Applicative]: WorkSampleValidator[F] = new WorkSampleValidator[F]()
}
