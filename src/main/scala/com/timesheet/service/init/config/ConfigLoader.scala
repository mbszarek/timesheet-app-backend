package com.timesheet.service.init.config

import com.timesheet.service.init.config.entities._

trait ConfigLoader[F[_]] {
  def loadHostConfig(): F[HostConfig]

  def loadInitConfig(): F[InitConfig]

  def loadMongoConfig(): F[MongoConfig]
}
