package com.timesheet
package core.store.worksamples.impl

import cats.data.OptionT
import com.timesheet.core.db.{MongoDriverMixin, MongoStoreUtils}
import com.timesheet.core.store.worksamples.WorkSamplesStoreAlgebra
import com.timesheet.model.db.ID
import com.timesheet.model.user.User
import com.timesheet.model.worksample.WorkSample
import monix.eval.Task
import reactivemongo.api.Cursor
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument

class WorkSamplesStoreMongo extends WorkSamplesStoreAlgebra[Task] with MongoDriverMixin with MongoStoreUtils {
  protected val collection: Task[BSONCollection] = getCollection("workSamples")

  override def create(workSample: WorkSample): Task[WorkSample] = {
    import WorkSample.workSampleHandler

    for {
      _ <- collection.executeOnCollection(implicit sc => _.insert.one(workSample))
    } yield workSample
  }

  override def update(workSample: WorkSample): OptionT[Task, WorkSample] =
    OptionT.liftF {
      for {
        selector <- getIdSelector(workSample.id)
        _        <- collection.executeOnCollection(implicit sc => _.update.one(selector, workSample))
      } yield workSample
    }

  override def get(id: ID): OptionT[Task, WorkSample] =
    OptionT {
      import WorkSample.workSampleHandler

      for {
        selector   <- getIdSelector(id)
        workSample <- collection.executeOnCollection(implicit sc => _.find(selector, None).one)
      } yield workSample
    }

  override def getAll(): Task[Seq[WorkSample]] = {
    import WorkSample.workSampleHandler

    collection.executeOnCollection { implicit sc =>
      _.find(BSONDocument(), None).cursor[WorkSample]().collect[Seq](-1, Cursor.FailOnError[Seq[WorkSample]]())
    }
  }

  override def delete(id: ID): OptionT[Task, WorkSample] =
    OptionT {
      import WorkSample.workSampleHandler

      for {
        selector   <- getIdSelector(id)
        workSample <- collection.executeOnCollection[Option[WorkSample]](implicit sc => _.find(selector, None).one)
        _          <- collection.executeOnCollection[Int](implicit sc => _.delete.element(workSample.get).map(_.limit))
      } yield workSample
    }

  override def getAllForUser(userId: User.UserId): Task[Seq[WorkSample]] =
    for {
      selector <- getUserIdSelector(userId)
      result <- collection.executeOnCollection { implicit sc =>
        _.find(selector, None).cursor[WorkSample]().collect[Seq](-1, Cursor.FailOnError[Seq[WorkSample]]())
      }
    } yield result
}
