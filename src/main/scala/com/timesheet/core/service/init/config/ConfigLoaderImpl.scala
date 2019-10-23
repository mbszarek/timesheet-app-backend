package com.timesheet.core.service.init.config

import cats.effect._
import com.timesheet.core.service.init.config.entities.{HostConfig, InitConfig}
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.catseffect._

final class ConfigLoaderImpl[F[_]: Sync] extends ConfigLoader[F] {
  def loadHostConfig(): F[HostConfig] =
    ConfigSource.default.at("hostConfig").loadF[F, HostConfig]

  def loadInitConfig(): F[InitConfig] =
    ConfigSource.default.at("init").loadF[F, InitConfig]
}

object ConfigLoaderImpl {
  def apply[F[_]: Sync]: ConfigLoaderImpl[F] = new ConfigLoaderImpl[F]()
}
