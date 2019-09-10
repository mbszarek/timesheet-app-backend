package com.timesheet.core.db

import monix.eval.Task
import monix.execution.Scheduler
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}

import scala.concurrent.Future
import scala.util.Try

trait MongoDriverMixin {
  import MongoDriverMixin._
  protected def connection: Task[MongoConnection] = Task.fromTry(Connection)

  protected def getDatabase(dbName: String): Task[DefaultDB] =
    connection
      .flatMap(conn => Task.deferFutureAction(implicit sc => conn.database(dbName)))

  protected def getCollection(collectionName: String): Task[BSONCollection] =
    getDatabase("Timesheet")
      .map(_.collection(collectionName))

  protected val collection: Task[BSONCollection]

//  protected def performOnCollection[A](action: (BSONCollection, Scheduler) => Future[A]): Task[A] =
//    collection.flatMap { coll =>
//      Task.deferFutureAction { implicit sc =>
//        action(coll, sc)
//      }
//    }
}

private object MongoDriverMixin {
  private[this] val Driver                                    = MongoDriver()
  private[this] val MongoUri: String                          = "mongodb://localhost:27017"
  private[this] val ParsedUri: Try[MongoConnection.ParsedURI] = MongoConnection.parseURI(MongoUri)
  val Connection: Try[MongoConnection] = for {
    parsedUri  <- ParsedUri
    connection <- Driver.connection(parsedUri, strictUri = true)
  } yield connection
}
