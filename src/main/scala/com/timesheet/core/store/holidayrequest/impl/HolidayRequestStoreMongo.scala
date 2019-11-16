package com.timesheet
package core.store.holidayrequest.impl

import java.time.LocalDate

import cats.implicits._
import cats.data._
import cats.effect._
import com.timesheet.core.store.base.StoreAlgebraImpl
import com.timesheet.core.store.holidayrequest.HolidayRequestStoreAlgebra
import com.timesheet.model.holidayrequest.{HolidayRequest, Status}
import com.timesheet.model.user.UserId
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters._

final class HolidayRequestStoreMongo[F[_]: ConcurrentEffect]
    extends StoreAlgebraImpl[F]
    with HolidayRequestStoreAlgebra[F] {
  import HolidayRequest._

  protected val collection: F[MongoCollection[HolidayRequest]] = getCollection("holidayRequest")

  override def getAllForUser(userId: UserId): F[List[HolidayRequest]] =
    for {
      coll <- collection
      holidayRequests <- coll
        .find(userIdRef equal userId)
        .compileFS2
        .toList
    } yield holidayRequests

  override def getAllForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[List[HolidayRequest]] =
    for {
      coll <- collection
      holidayRequests <- coll
        .find(
          and(
            userIdRef equal userId,
            dateRef gte fromDate,
            dateRef lte toDate,
          ),
        )
        .compileFS2
        .toList
    } yield holidayRequests

  override def getAllBetweenDates(
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[List[HolidayRequest]] =
    for {
      coll <- collection
      holidayRequests <- coll
        .find(
          and(
            dateRef gte fromDate,
            dateRef lte toDate,
          ),
        )
        .compileFS2
        .toList
    } yield holidayRequests

  override def getAllPendingForUser(userId: UserId): F[List[HolidayRequest]] =
    for {
      coll <- collection
      holidayRequests <- coll
        .find(
          and(
            userIdRef equal userId,
            statusRef equal Status.Pending,
          ),
        )
        .compileFS2
        .toList
    } yield holidayRequests

  override def getAllPendingForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[List[HolidayRequest]] =
    for {
      coll <- collection
      holidayRequests <- coll
        .find(
          and(
            userIdRef equal userId,
            dateRef gte fromDate,
            dateRef lte toDate,
            statusRef equal Status.Pending,
          ),
        )
        .compileFS2
        .toList
    } yield holidayRequests

  override def deleteUserHolidayRequestForDate(
    userId: UserId,
    date: LocalDate,
  ): OptionT[F, HolidayRequest] =
    OptionT {
      for {
        coll <- collection
        holidayRequest <- coll
          .findOneAndDelete(and(userIdRef equal userId, dateRef equal date))
          .compileFS2
          .last
      } yield holidayRequest
    }

  override def countPendingForUserForDateRange(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[Long] =
    for {
      coll <- collection
      result <- coll
        .countDocuments(
          and(
            userIdRef equal userId,
            dateRef gte fromDate,
            dateRef lte toDate,
            statusRef equal Status.Pending,
          ),
        )
        .compileFS2
        .lastOrError
    } yield result
}

object HolidayRequestStoreMongo {
  def apply[F[_]: ConcurrentEffect]: HolidayRequestStoreMongo[F] = new HolidayRequestStoreMongo[F]
}
