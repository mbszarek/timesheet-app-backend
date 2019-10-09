package com.timesheet.core.validation.worksample.impl

import cats._
import cats.data._
import com.timesheet.core.validation.ValidationUtils.{WorkSampleValidationError, WrongUserState}
import com.timesheet.core.validation.worksample.WorkSampleValidatorAlgebra
import com.timesheet.model.user.User
import com.timesheet.model.worksample._

final class WorkSampleValidator[F[_]: Applicative] extends WorkSampleValidatorAlgebra[F] {
  def hasUserCorrectState(user: User, activityType: ActivityType): EitherT[F, WorkSampleValidationError, Unit] = {
    lazy val wrongEither: EitherT[F, WorkSampleValidationError, Unit] = EitherT.leftT[F, Unit](WrongUserState)
    lazy val properEither: EitherT[F, WorkSampleValidationError, Unit] =
      EitherT.rightT[F, WorkSampleValidationError](())

    user.isCurrentlyAtWork.fold(properEither) { cond =>
      activityType match {
        case Entrance =>
          if (cond) wrongEither
          else
            properEither

        case Departure =>
          if (cond) properEither
          else
            wrongEither
      }
    }
  }
}

object WorkSampleValidator {
  def apply[F[_]: Applicative]: WorkSampleValidator[F] = new WorkSampleValidator[F]()
}
