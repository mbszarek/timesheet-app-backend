package com.timesheet.service.init.config

import cats.effect._
import com.timesheet.service.init.config.entities._
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.catseffect._

final class ConfigLoaderImpl[F[_]: Sync] extends ConfigLoader[F] {
  def loadHostConfig(): F[HostConfig] =
    ConfigSource.default.at("hostConfig").loadF[F, HostConfig]

  def loadInitConfig(): F[entities.InitConfig] =
    ConfigSource.default.at("init").loadF[F, InitConfig]

  def loadMongoConfig(): F[MongoConfig] =
    ConfigSource.default.at("mongoConfig").loadF[F, MongoConfig]
}

object ConfigLoaderImpl {
  def apply[F[_]: Sync]: ConfigLoaderImpl[F] = new ConfigLoaderImpl[F]()
}
