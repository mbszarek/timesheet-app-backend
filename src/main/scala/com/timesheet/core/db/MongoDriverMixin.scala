package com.timesheet.core.db

import cats.implicits._
import com.timesheet.concurrent.FutureConcurrentEffect
import monix.eval.Task
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}

import scala.util.Try

trait MongoDriverMixin[F[_]] {
  import MongoDriverMixin._
  protected def connection(implicit F: FutureConcurrentEffect[F]): F[MongoConnection] =
    F.async(_.apply(Connection.toEither))

  protected def getDatabase(dbName: String)(implicit F: FutureConcurrentEffect[F]): F[DefaultDB] =
    for {
      conn <- connection
      db   <- F.wrapFuture(implicit sc => conn.database(dbName))
    } yield db

  protected def getCollection(collectionName: String)(implicit F: FutureConcurrentEffect[F]): F[BSONCollection] =
    getDatabase("Timesheet")
      .map(_.collection(collectionName))

  protected val collection: F[BSONCollection]

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
