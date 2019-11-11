package com.timesheet
package core.store.holiday.impl

import java.time.LocalDate

import cats._
import cats.data._
import cats.implicits._
import cats.effect._
import com.timesheet.core.db.MongoDriverMixin
import com.timesheet.core.store.holiday.HolidayStoreAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.holiday.Holiday
import com.timesheet.model.user.UserId
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters._

final class HolidayStoreMongo[F[_]: ConcurrentEffect] extends HolidayStoreAlgebra[F] with MongoDriverMixin[F] {
  override type T = Holiday

  protected val collection: F[MongoCollection[Holiday]] = getCollection("holidays")

  override def create(holiday: Holiday): F[Holiday] =
    for {
      coll <- collection
      _ <- coll
        .insertOne(holiday)
        .compileFS2
        .drain
    } yield holiday

  override def update(holiday: Holiday): OptionT[F, Holiday] =
    OptionT.liftF {
      for {
        coll <- collection
        _ <- coll
          .findOneAndReplace(equal("_id", holiday.id.value), holiday)
          .compileFS2
          .drain
      } yield holiday
    }

  override def get(id: ID): OptionT[F, Holiday] =
    OptionT {
      for {
        coll <- collection
        holiday <- coll
          .find(equal("_id", id.value))
          .compileFS2
          .last
      } yield holiday
    }

  override def getAll(): F[List[Holiday]] =
    for {
      coll <- collection
      holidays <- coll
        .find()
        .compileFS2
        .toList
    } yield holidays

  override def getAllForUser(userId: UserId): F[List[Holiday]] =
    for {
      coll <- collection
      holidays <- coll
        .find(equal("userId", userId.value))
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
            equal("userId", userId.value),
            gte("date", fromDate.toInstant().toEpochMilli),
            lte("date", toDate.toInstant().toEpochMilli),
          ),
        )
        .compileFS2
        .toList
    } yield holidays

  override def delete(id: ID): OptionT[F, Holiday] =
    OptionT {
      for {
        coll <- collection
        holiday <- coll
          .findOneAndDelete(equal("_id", id.value))
          .compileFS2
          .last
      } yield holiday
    }

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
              equal("userId", userId),
              equal("date", date),
            ),
          )
          .compileFS2
          .last
      } yield holiday
    }
}

object HolidayStoreMongo {
  def apply[F[_]: ConcurrentEffect]: HolidayStoreMongo[F] = new HolidayStoreMongo[F]
}
