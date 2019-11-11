package com.timesheet

import java.time.LocalDate

import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.QueryParamDecoderMatcher

object EndpointUtils {
  implicit val dateQueryParamDecoder: QueryParamDecoder[LocalDate] =
    QueryParamDecoder[String].map(LocalDate.parse)

  object FromLocalDateMatcher extends QueryParamDecoderMatcher[LocalDate]("from")

  object ToLocalDateMatcher extends QueryParamDecoderMatcher[LocalDate]("to")
}
