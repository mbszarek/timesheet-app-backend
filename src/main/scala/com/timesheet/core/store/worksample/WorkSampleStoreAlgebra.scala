package com.timesheet.core.store.worksample

import java.time.LocalDateTime

import cats.data._
import com.avsystem.commons.mongo.BsonRef
import com.avsystem.commons.serialization.GenCodec
import com.timesheet.core.store.base.StoreAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.user.{User, UserId}
import com.timesheet.model.work.WorkSample

import scala.reflect.ClassTag

trait WorkSampleStoreAlgebra[F[_]] extends StoreAlgebra[F] {

  override type K = WorkSample

  protected def tag: ClassTag[WorkSample]   = implicitly
  protected def codec: GenCodec[WorkSample] = implicitly
  protected def idRef: BsonRef[WorkSample, ID] = implicitly

  def getAllForUserBetweenDates(
    userId: UserId,
    from: LocalDateTime,
    to: LocalDateTime,
  ): F[List[WorkSample]]

  def wasAtWork(
    user: User,
    date: LocalDateTime,
  ): F[Boolean]

  def getAllForUser(userId: UserId): F[List[WorkSample]]
}
