package com.timesheet.core.service.holidayrequest.impl

import java.time.LocalDate

import cats.data.EitherT
import cats.effect.Concurrent
import cats.implicits._
import com.timesheet.core.service.base.EntityServiceAlgebra.EntityStore
import com.timesheet.core.service.base.impl.EntityServiceImpl
import com.timesheet.core.service.holidayrequest.HolidayRequestServiceAlgebra
import com.timesheet.core.store.holidayrequest.HolidayRequestStoreAlgebra
import com.timesheet.core.validation.ValidationUtils.{BasicError, DateValidationError, EntityNotFound, EntityRemovalError, HolidayRequestNotFound}
import com.timesheet.core.validation.date.DateValidatorAlgebra
import com.timesheet.core.validation.holiday.HolidayValidatorAlgebra
import com.timesheet.core.validation.user.UserValidatorAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.holiday.HolidayType
import com.timesheet.model.holidayrequest.{HolidayRequest, Status}
import com.timesheet.model.user.{User, UserId}

final class HolidayRequestService[F[_]: Concurrent](
  userValidator: UserValidatorAlgebra[F],
  dateValidator: DateValidatorAlgebra[F],
  holidayValidator: HolidayValidatorAlgebra[F],
  holidayRequestStore: HolidayRequestStoreAlgebra[F],
) extends EntityServiceImpl[F] with HolidayRequestServiceAlgebra[F] {

  override protected def entityStore: EntityStore[F, HolidayRequest] = holidayRequestStore

  override def createHolidayRequest(
    user: User,
    fromDate: LocalDate,
    toDate: LocalDate,
    holidayType: HolidayType,
    description: String,
  ): EitherT[F, DateValidationError, HolidayRequest] =
    for {
      _ <- dateValidator.isDateInTheFuture(fromDate.atStartOfDay())
      _ <- dateValidator.areDatesInProperOrder(fromDate.atStartOfDay(), toDate.atStartOfDay())

      _ <- holidayValidator
        .checkIfUserCanRequestHoliday(user, fromDate, toDate, holidayType)
        .leftWiden[DateValidationError]

      holidayRequest <- EitherT
        .right[DateValidationError](
          holidayRequestStore.create(createHolidayRequestEntity(user.id, fromDate, toDate, holidayType, description)),
        )
    } yield holidayRequest

  override def deleteHolidayRequest(
    user: User,
    id: ID,
  ): EitherT[F, BasicError, HolidayRequest] =
    for {
      entity <- holidayRequestStore
        .get(id)
        .toRight[BasicError](EntityNotFound(id))

      _ <- userValidator
        .canModifyResource(user, entity)
        .leftWiden[BasicError]

      _ <- holidayRequestStore
        .delete(id)
        .toRight[BasicError](EntityRemovalError(id))
    } yield entity

  override def getAllHolidayRequests(): F[List[HolidayRequest]] =
    holidayRequestStore
      .getAll()

  override def getAllHolidayRequestsForSpecifiedDateRange(
    fromDate: LocalDate,
    toDate: LocalDate,
  ): EitherT[F, DateValidationError, List[HolidayRequest]] =
    for {
      _ <- dateValidator
        .areDatesInProperOrder(fromDate.atStartOfDay(), toDate.atStartOfDay())

      holidayRequests <- EitherT
        .right[DateValidationError] {
          holidayRequestStore.getAllBetweenDates(fromDate, toDate)
        }
    } yield holidayRequests

  override def getAllHolidayRequestsForUser(userId: UserId): F[List[HolidayRequest]] =
    holidayRequestStore.getAllForUser(userId)

  override def getAllPendingHolidayRequestsForUser(userId: UserId): F[List[HolidayRequest]] =
    holidayRequestStore.getAllPendingForUser(userId)

  override def getAllHolidayRequestsForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): EitherT[F, DateValidationError, List[HolidayRequest]] =
    for {
      _ <- dateValidator.areDatesInProperOrder(fromDate.atStartOfDay(), toDate.atStartOfDay())

      holidayRequests <- EitherT.right[DateValidationError] {
        holidayRequestStore.getAllForUserBetweenDates(userId, fromDate, toDate)
      }
    } yield holidayRequests

  override def getAllPendingHolidayRequestsForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): EitherT[F, DateValidationError, List[HolidayRequest]] =
    for {
      _ <- dateValidator
        .areDatesInProperOrder(fromDate.atStartOfDay(), toDate.atStartOfDay())

      holidayRequests <- EitherT.right[DateValidationError] {
        holidayRequestStore.getAllPendingForUserBetweenDates(userId, fromDate, toDate)
      }
    } yield holidayRequests

  private def createHolidayRequestEntity(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
    holidayType: HolidayType,
    description: String,
  ): HolidayRequest =
    HolidayRequest(
      ID.createNew(),
      userId,
      fromDate,
      toDate,
      holidayType,
      description,
      Status.Pending,
    )
}

object HolidayRequestService {
  def apply[F[_]: Concurrent](
    userValidator: UserValidatorAlgebra[F],
    dateValidator: DateValidatorAlgebra[F],
    holidayValidator: HolidayValidatorAlgebra[F],
    holidayRequestStore: HolidayRequestStoreAlgebra[F],
  ): HolidayRequestService[F] =
    new HolidayRequestService[F](userValidator, dateValidator, holidayValidator, holidayRequestStore)
}
