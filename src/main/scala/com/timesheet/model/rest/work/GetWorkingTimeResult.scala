package com.timesheet.model.rest.work

final case class GetWorkingTimeResult(
  workingTimeSeconds: Long,
  obligatoryWorkTimeSeconds: Long)
