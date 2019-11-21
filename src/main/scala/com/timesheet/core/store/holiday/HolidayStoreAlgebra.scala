package com.timesheet.core.store.holiday

import java.time.LocalDate

import cats.data._
import com.avsystem.commons.mongo.BsonRef
import com.avsystem.commons.serialization.GenCodec
import com.timesheet.core.store.base.StoreAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.holiday.Holiday
import com.timesheet.model.user.UserId

import scala.reflect.ClassTag

trait HolidayStoreAlgebra[F[_]] extends StoreAlgebra[F] {
  override type Entity = Holiday

  protected def tag: ClassTag[Holiday]      = implicitly
  protected def codec: GenCodec[Holiday]    = implicitly
  protected def idRef: BsonRef[Holiday, ID] = implicitly

  def getAllForUser(userId: UserId): F[List[Holiday]]

  def getAllForUserBetweenDates(
    userId: UserId,
    fromDate: LocalDate,
    toDate: LocalDate,
  ): F[List[Holiday]]
}
