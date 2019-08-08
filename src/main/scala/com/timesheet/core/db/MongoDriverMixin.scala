package com.timesheet.core.db

import monix.eval.Task
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}

import scala.util.Try

trait MongoDriverMixin {
  import MongoDriverMixin._
  def connection: Task[MongoConnection] = Task.fromTry(Connection)
  def database(dbName: String): Task[DefaultDB] =
    connection
      .flatMap(conn => Task.deferFutureAction(implicit sc => conn.database(dbName)))
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
