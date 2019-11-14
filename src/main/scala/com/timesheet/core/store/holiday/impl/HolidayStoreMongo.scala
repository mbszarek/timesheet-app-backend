package com.timesheet
package core.store.holiday.impl

import java.time.LocalDate

import cats.data._
import cats.implicits._
import cats.effect._
import com.timesheet.core.store.base.StoreAlgebraImpl
import com.timesheet.core.store.holiday.HolidayStoreAlgebra
import com.timesheet.model.holiday.Holiday
import com.timesheet.model.user.UserId
import org.mongodb.scala.MongoCollection
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
            dateRef gte fromDate,
            dateRef lte toDate,
          ),
        )
        .compileFS2
        .toList
    } yield holidays

  override def deleteUserHolidayForDate(
    userId: UserId,
    date: LocalDate,
  ): OptionT[F, Holiday] =
    OptionT {
      for {
        coll <- collection
        holiday <- coll
          .findOneAndDelete(
            and(
              userIdRef equal userId,
              dateRef equal date,
            ),
          )
          .compileFS2
          .last
      } yield holiday
    }

  override def countForUserForDateRange(
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
          ),
        )
        .compileFS2
        .lastOrError
    } yield result
}

object HolidayStoreMongo {
  def apply[F[_]: ConcurrentEffect]: HolidayStoreMongo[F] = new HolidayStoreMongo[F]
}
