package com.timesheet.model.rest.work

final case class GetWorkingTimeResultDTO(
  workingTimeSeconds: Long,
  obligatoryWorkTimeSeconds: Long,
)
