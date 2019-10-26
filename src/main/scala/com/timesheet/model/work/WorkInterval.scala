package com.timesheet.model.work

import java.time.LocalDateTime

final case class WorkInterval(
  from: LocalDateTime,
  to: LocalDateTime,
  wasAtWork: Boolean,
)
