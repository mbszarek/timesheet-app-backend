package com.timesheet.core.db

import cats.implicits._
import cats.effect._
import com.avsystem.commons.mongo.core.GenCodecRegistry
import com.avsystem.commons.serialization.GenCodec
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase}

import scala.reflect.ClassTag

trait MongoDriverMixin[F[_]] {
  import MongoDriverMixin._

  type T

  protected def connection(implicit F: Sync[F]): F[MongoClient] =
    F.delay(Client)

  protected def getDatabase(dbName: String)(implicit F: Sync[F]): F[MongoDatabase] =
    for {
      conn <- connection
    } yield conn.getDatabase(dbName)

  protected def getCollection(
    collectionName: String
  )(implicit F: Sync[F], T: GenCodec[T], C: ClassTag[T]): F[MongoCollection[T]] =
    for {
      db <- getDatabase("Timesheet")
    } yield createCollection(db, collectionName)

  protected val collection: F[MongoCollection[T]]

  private def createCollection[T: GenCodec: ClassTag](db: MongoDatabase, name: String): MongoCollection[T] = {
    val newRegistry = GenCodecRegistry.create[T](db.codecRegistry, GenCodecRegistry.LegacyOptionEncoding)
    db.withCodecRegistry(newRegistry).getCollection(name)
  }
}

private object MongoDriverMixin {
  private[this] val MongoUri: String = "mongodb://localhost:27017"
  private val Client: MongoClient    = MongoClient(MongoUri)
}
