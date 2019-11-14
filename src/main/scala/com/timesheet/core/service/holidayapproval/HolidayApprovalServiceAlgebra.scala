package com.timesheet.core.service.holidayapproval

import com.timesheet.model.holiday.Holiday
import com.timesheet.model.holidayrequest.HolidayRequest
import com.timesheet.model.user.User

trait HolidayApprovalServiceAlgebra[F[_]] {
  def approveHolidays(
    approvedBy: User,
    holidayRequest: HolidayRequest,
  ): F[Holiday]

  def denyHolidays(
    deniedBy: User,
    reason: Option[String],
    holidayRequest: HolidayRequest,
  ): F[Option[HolidayRequest]]
}
