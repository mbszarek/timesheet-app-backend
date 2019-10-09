package com.timesheet.core.service.worksample.impl

import java.time.Instant

import cats.implicits._
import cats.data._
import com.timesheet.concurrent.FutureConcurrentEffect
import com.timesheet.core.service.worksample.WorkSampleServiceAlgebra
import com.timesheet.core.store.user.impl.UserStoreMongo
import com.timesheet.core.store.worksample.WorkSampleStoreAlgebra
import com.timesheet.core.validation.ValidationUtils
import com.timesheet.core.validation.ValidationUtils.{WorkSampleValidationError, WrongUserState}
import com.timesheet.core.validation.worksample.WorkSampleValidatorAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId
import com.timesheet.model.worksample.{ActivityType, Departure, Entrance, WorkSample}

class WorkSampleService[F[_]: FutureConcurrentEffect](
  userStore: UserStoreMongo[F],
  workSampleStore: WorkSampleStoreAlgebra[F],
  workSampleValidator: WorkSampleValidatorAlgebra[F],
) extends WorkSampleServiceAlgebra[F] {
  def tagWorkerEntrance(user: User): EitherT[F, WorkSampleValidationError, WorkSample] =
    for {
      _          <- workSampleValidator.hasUserCorrectState(user, Entrance)
      _          <- changeUserStatus(user, defaultValue = true)
      workSample <- EitherT.liftF(workSampleStore.create(createWorkSample(user.id, Entrance)))
    } yield workSample

  def tagWorkerExit(user: User): EitherT[F, WorkSampleValidationError, WorkSample] =
    for {
      _          <- workSampleValidator.hasUserCorrectState(user, Departure)
      _          <- changeUserStatus(user, defaultValue = false)
      workSample <- EitherT.liftF(workSampleStore.create(createWorkSample(user.id, Departure)))
    } yield workSample

  private def createWorkSample(userId: UserId, activityType: ActivityType): WorkSample = WorkSample(
    ID.createNew(),
    userId,
    activityType,
    Instant.now(),
  )

  private def changeUserStatus(
    user: User,
    defaultValue: Boolean,
  ): EitherT[F, ValidationUtils.WrongUserState.type, User] =
    EitherT.fromOptionF(
      userStore.update(user.copy(isCurrentlyAtWork = user.isCurrentlyAtWork.map(!_).orElse(defaultValue.some))).value,
      WrongUserState,
    )
}

object WorkSampleService {
  def apply[F[_]: FutureConcurrentEffect](
    userStoreMongo: UserStoreMongo[F],
    workSampleStore: WorkSampleStoreAlgebra[F],
    workSampleValidator: WorkSampleValidatorAlgebra[F]
  ): WorkSampleService[F] =
    new WorkSampleService[F](userStoreMongo, workSampleStore, workSampleValidator)
}
