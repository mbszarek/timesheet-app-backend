package com.timesheet
package core.store.holidayrequest.impl

import java.time.LocalDate

import cats.implicits._
import cats.effect._
import com.timesheet.service.init.config.entities.MongoConfig
import com.timesheet.core.store.base.StoreAlgebraImpl
import com.timesheet.core.store.holidayrequest.HolidayRequestStoreAlgebra
import com.timesheet.model.holidayrequest.{HolidayRequest, Status}
import com.timesheet.model.user.UserId
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._

final class HolidayRequestStoreMongo[F[_]: ConcurrentEffect](implicit protected val mongoConfig: MongoConfig)
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
            getBetweenDatesQuery(fromDate, toDate),
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
          getBetweenDatesQuery(fromDate, toDate),
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
            getBetweenDatesQuery(fromDate, toDate),
            statusRef equal Status.Pending,
          ),
        )
        .compileFS2
        .toList
    } yield holidayRequests

  private def getBetweenDatesQuery(
    fromDate: LocalDate,
    toDate: LocalDate,
  ): Bson =
    or(
      and(
        toDateRef gte fromDate,
        toDateRef lte toDate,
      ),
      and(
        fromDateRef lte toDate,
        fromDateRef gte fromDate,
      ),
      and(
        toDateRef gte toDate,
        fromDateRef lte fromDate,
      ),
    )
}

object HolidayRequestStoreMongo {
  def apply[F[_]: ConcurrentEffect](implicit mongoConfig: MongoConfig): HolidayRequestStoreMongo[F] =
    new HolidayRequestStoreMongo[F]
}
