package com.timesheet.core.service.init.config

import com.timesheet.core.service.init.config.entities.{HostConfig, InitConfig}

trait ConfigLoader[F[_]] {
  def loadHostConfig(): F[HostConfig]

  def loadInitConfig(): F[InitConfig]
}
