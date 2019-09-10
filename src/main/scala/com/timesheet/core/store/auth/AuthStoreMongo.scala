package com.timesheet.core.store.auth

import java.time.Instant

import cats.data.OptionT
import com.timesheet.core.db.MongoDriverMixin
import com.timesheet.model.user.User.UserId
import monix.eval.Task
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, document}
import tsec.authentication.{AugmentedJWT, BackingStore}
import tsec.common.SecureRandomId
import tsec.jws.JWSSerializer
import tsec.jws.mac.{JWSMacCV, JWSMacHeader, JWTMacImpure}
import tsec.mac.jca.{MacErrorM, MacSigningKey}

class AuthStoreMongo[A](key: MacSigningKey[A])(implicit hs: JWSSerializer[JWSMacHeader[A]], s: JWSMacCV[MacErrorM, A])
    extends BackingStore[Task, SecureRandomId, AugmentedJWT[A, UserId]]
    with MongoDriverMixin {
  import AuthStoreMongo._

  private def collection: Task[BSONCollection] = getCollection("auth")

  override def put(elem: AugmentedJWT[A, UserId]): Task[AugmentedJWT[A, UserId]] =
    for {
//      _ <- performOnCollection { (coll, sc) =>
//        coll.insert.one(elem)(sc)
//      }
      _ <- collection.flatMap { coll =>
        Task.deferFutureAction { implicit sc =>
          coll.insert.one(elem)
        }
      }
    } yield elem

  override def update(v: AugmentedJWT[A, UserId]): Task[AugmentedJWT[A, UserId]] =
    for {
      selector <- getIdSelector(v.id)
      _ <- collection.flatMap { coll =>
        Task.deferFutureAction { implicit sc =>
          coll.update.one(selector, v)
        }
      }
    } yield v

  override def delete(id: SecureRandomId): Task[Unit] =
    for {
      selector <- getIdSelector(id)
      _ <- collection.flatMap { coll =>
        Task.deferFutureAction { implicit sc =>
          coll.delete.one(selector)
        }
      }
    } yield ()

  override def get(id: SecureRandomId): OptionT[Task, AugmentedJWT[A, UserId]] = {

    implicit val jwtReader: BSONDocumentReader[AugmentedJWT[A, UserId]] = {
      import com.timesheet.core.db.BSONInstances.instantReader

      (bson: BSONDocument) =>
        {
          val value = for {
            jwt      <- bson.getAs[String]("jwt")
            identity <- bson.getAs[Long]("identity")
            expiry   <- bson.getAs[Instant]("expiry")
          } yield (jwt, identity, expiry)

          val lastTouched = bson.getAs[Instant]("last_touched")

          value.fold(throw new Exception()) {
            case (jwtStringify, identity, expiry) =>
              JWTMacImpure.verifyAndParse(jwtStringify, key) match {
                case Left(err) => throw new Exception(err)
                case Right(jwt) =>
                  AugmentedJWT(id, jwt, UserId(identity), expiry, lastTouched)
              }
          }
        }
    }

    OptionT {
      for {
        selector <- getIdSelector(id)
        entity <- collection.flatMap { coll =>
          Task.deferFutureAction { implicit sc =>
            val x = coll.find(selector, None).one
            x.onComplete(println)
            x
          }
        }
      } yield entity
    }
  }

  private def getIdSelector(id: SecureRandomId): Task[BSONDocument] = Task.pure {
    import com.timesheet.core.db.BSONInstances.secureRandomIdWriter

    document(
      "id" -> id
    )
  }
}

object AuthStoreMongo {
  import com.timesheet.core.db.BSONInstances._

  def apply[A](
    key: MacSigningKey[A]
  )(implicit hs: JWSSerializer[JWSMacHeader[A]], s: JWSMacCV[MacErrorM, A]): AuthStoreMongo[A] =
    new AuthStoreMongo(key)(hs, s)

  implicit def jwtWriter[A](implicit hs: JWSSerializer[JWSMacHeader[A]]): BSONDocumentWriter[AugmentedJWT[A, UserId]] =
    (jwt: AugmentedJWT[A, UserId]) =>
      BSONDocument(
        "id"           -> jwt.id,
        "jwt"          -> jwt.jwt.toEncodedString,
        "identity"     -> jwt.identity.id,
        "expiry"       -> jwt.expiry,
        "last_touched" -> jwt.lastTouched,
    )
}
