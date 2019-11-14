package com.timesheet.core.service.holidayapproval.impl

import cats.implicits._
import cats.effect._
import com.timesheet.core.service.holidayapproval.HolidayApprovalServiceAlgebra
import com.timesheet.core.store.holiday.HolidayStoreAlgebra
import com.timesheet.core.store.holidayrequest.HolidayRequestStoreAlgebra
import com.timesheet.model.holiday.Holiday
import com.timesheet.model.holidayrequest.HolidayRequest
import com.timesheet.model.user.User

final class HolidayApprovalService[F[_]: Sync](
  holidayStore: HolidayStoreAlgebra[F],
  holidayRequestStore: HolidayRequestStoreAlgebra[F],
) extends HolidayApprovalServiceAlgebra[F] {

  override def approveHolidays(
    approvedBy: User,
    holidayRequest: HolidayRequest,
  ): F[Holiday] =
    for {
      holiday <- holidayStore
        .create {
          holidayRequest.toHoliday
        }
      _ <- holidayRequestStore
        .delete(holidayRequest.id)
        .value
    } yield holiday

  override def denyHolidays(
    deniedBy: User,
    reason: Option[String],
    holidayRequest: HolidayRequest,
  ): F[Option[HolidayRequest]] =
    holidayRequestStore.update {
      holidayRequest.deny(deniedBy.id, reason)
    }.value
}

object HolidayApprovalService {
  def apply[F[_]: Sync](
    holidayStore: HolidayStoreAlgebra[F],
    holidayRequestStore: HolidayRequestStoreAlgebra[F],
  ): HolidayApprovalService[F] = new HolidayApprovalService[F](holidayStore, holidayRequestStore)
}
