package com.timesheet.model.db

import com.timesheet.model.user.UserId

trait DBEntityWithUserId {
  val userId: UserId
}
