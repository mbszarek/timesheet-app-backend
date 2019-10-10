package com.timesheet
package core.store.worksample.impl

import java.time.Instant

import cats.implicits._
import cats.data.OptionT
import com.timesheet.concurrent.FutureConcurrentEffect
import com.timesheet.core.db.{MongoDriverMixin, MongoStoreUtils}
import com.timesheet.core.store.worksample.WorkSampleStoreAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.user.User
import com.timesheet.model.user.User.UserId
import com.timesheet.model.worksample.WorkSample
import reactivemongo.api.Cursor
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, document}

class WorkSampleStoreMongo[F[_]: FutureConcurrentEffect]
    extends WorkSampleStoreAlgebra[F]
    with MongoDriverMixin[F]
    with MongoStoreUtils[F] {
  protected val collection: F[BSONCollection] = getCollection("workSamples")

  def create(workSample: WorkSample): F[WorkSample] = {
    import WorkSample.workSampleHandler

    for {
      _ <- collection.executeOnCollection(implicit sc => _.insert.one(workSample))
    } yield workSample
  }

  def update(workSample: WorkSample): OptionT[F, WorkSample] =
    OptionT.liftF {
      for {
        selector <- getIdSelector(workSample.id)
        _        <- collection.executeOnCollection(implicit sc => _.update.one(selector, workSample))
      } yield workSample
    }

  def get(id: ID): OptionT[F, WorkSample] =
    OptionT {
      import WorkSample.workSampleHandler

      for {
        selector   <- getIdSelector(id)
        workSample <- collection.executeOnCollection(implicit sc => _.find(selector, None).one)
      } yield workSample
    }

  def getAllForUserBetweenDates(userId: UserId, from: Instant, to: Instant): F[Seq[WorkSample]] =
    for {
      selector    <- getIdAndDateSelector(userId, from, to)
      workSamples <- collection.executeOnCollection(implicit sc => _.findSeq[WorkSample](selector))
    } yield workSamples

  def getAll(): F[Seq[WorkSample]] = {
    import WorkSample.workSampleHandler

    collection.executeOnCollection { implicit sc =>
      _.findSeq[WorkSample](document())
    }
  }

  def delete(id: ID): OptionT[F, WorkSample] =
    OptionT {
      import WorkSample.workSampleHandler

      for {
        selector   <- getIdSelector(id)
        workSample <- collection.executeOnCollection[Option[WorkSample]](implicit sc => _.find(selector, None).one)
        _          <- collection.executeOnCollection[Int](implicit sc => _.delete.element(workSample.get).map(_.limit))
      } yield workSample
    }

  def getAllForUser(userId: User.UserId): F[Seq[WorkSample]] =
    for {
      selector <- getUserIdSelector(userId)
      result <- collection.executeOnCollection { implicit sc =>
        _.find(selector, None).cursor[WorkSample]().collect[Seq](-1, Cursor.FailOnError[Seq[WorkSample]]())
      }
    } yield result

  private def getIdAndDateSelector(userId: UserId, from: Instant, to: Instant): F[BSONDocument] = {
    import com.timesheet.core.db.BSONInstances.instantHandler

    document(
      "_id" -> userId,
      "date" -> document(
        "$gte" -> from,
        "$lt"  -> to,
      )
    )
  }.pure[F]
}

object WorkSampleStoreMongo {
  def apply[F[_]: FutureConcurrentEffect]: WorkSampleStoreMongo[F] = new WorkSampleStoreMongo[F]()
}
