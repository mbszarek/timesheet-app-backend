package com.timesheet.core.config

import cats.effect._
import com.timesheet.core.config.entities.HostConfig
import pureconfig._
import pureconfig.generic.auto._

final class ConfigLoader[F[_]: Sync] {
  def loadHostConfig(): fs2.Stream[F, HostConfig] =
    fs2.Stream.eval {
      Sync[F].delay {
        ConfigSource.default.loadOrThrow[HostConfig]
      }
    }
}

object ConfigLoader {
  def apply[F[_]: Sync]: ConfigLoader[F] = new ConfigLoader[F]()
}
