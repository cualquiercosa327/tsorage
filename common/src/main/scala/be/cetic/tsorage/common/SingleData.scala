package be.cetic.tsorage.common

import java.time.LocalDateTime

case class SingleData(timeSeries: TimeSeries, datetime: LocalDateTime, value: BigDecimal)
