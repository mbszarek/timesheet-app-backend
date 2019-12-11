package com.timesheet.service.init.config.entities

final case class HostConfig(
  port: Int,
  hostName: String,
  logHeaders: Boolean,
  logBody: Boolean,
)
