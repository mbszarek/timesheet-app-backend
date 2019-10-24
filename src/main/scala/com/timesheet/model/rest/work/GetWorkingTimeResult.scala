package com.timesheet.model.rest.work

final case class GetWorkingTimeResult(
  workingTime: WorkTime,
  obligatoryWorkTime: WorkTime
)
