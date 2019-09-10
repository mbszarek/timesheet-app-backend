package com.timesheet.endpoint

import cats.Applicative
import cats.effect.Sync
import io.circe.{Encoder, Json}
import org.http4s.{EntityEncoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.jsonEncoderOf

class TestEndpoint[F[_]: Sync] extends Http4sDsl[F] {
  import TestEndpoint._

  def testEndpoint(): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / name =>
      Ok(Test(s"Test: $name"))
  }
}

object TestEndpoint {
  def apply[F[_]: Sync]: HttpRoutes[F] = new TestEndpoint[F].testEndpoint()

  final case class Test(value: String) extends AnyVal

  implicit val testEncoder: Encoder[Test] = (test: Test) =>
    Json.obj(
      ("value", Json.fromString(test.value))
  )

  implicit def testEntityEncoder[F[_]: Applicative]: EntityEncoder[F, Test] = jsonEncoderOf[F, Test]
}
