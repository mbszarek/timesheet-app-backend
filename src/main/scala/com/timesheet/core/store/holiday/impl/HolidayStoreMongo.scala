package com.timesheet
package core.store.holiday.impl

import java.time.LocalDate

import cats.implicits._
import cats.effect._
import com.timesheet.core.store.base.StoreAlgebraImpl
import com.timesheet.core.store.holiday.HolidayStoreAlgebra
import com.timesheet.model.holiday.Holiday
import com.timesheet.model.user.UserId
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._

final class HolidayStoreMongo[F[_]: ConcurrentEffect] extends StoreAlgebraImpl[F] with HolidayStoreAlgebra[F] {
  import Holiday._

  protected val collection: F[MongoCollection[Holiday]] = getCollection("holidays")

  override def getAllForUser(userId: UserId): F[List[Holiday]] =
    for {
      coll <- collection
      holidays <- coll
        .find(userIdRef equal userId)
        .compileFS2
        .toList
    } yield holidays

  override def getAllForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[List[Holiday]] =
    for {
      coll <- collection
      holidays <- coll
        .find(
          and(
            userIdRef equal userId,
            getBetweenDatesQuery(fromDate, toDate),
          ),
        )
        .compileFS2
        .toList
    } yield holidays

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

object HolidayStoreMongo {
  def apply[F[_]: ConcurrentEffect]: HolidayStoreMongo[F] = new HolidayStoreMongo[F]
}
