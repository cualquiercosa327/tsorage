package be.cetic.tsorage.common

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import java.util.Date

object DateTimeConverter {
  /**
   * Convert a string in ISO 8601 format to a [[java.time.LocalDateTime]].
   *
   * Note that the time offsets from UTC (time zone) are completely ignored. For example:
   * {{{
   * scala> import be.cetic.tsorage.common.DateTimeConverter
   * scala> val withoutUtc = "2019-09-20T20:20:00.000Z"
   * scala> DateTimeConverter.strToLocalDateTime(withoutUtc)
   * 2019-09-20T20:20
   * scala> val withUtc = "2019-09-20T20:20:00+02:00"
   * scala> DateTimeConverter.strToLocalDateTime(withUtc)
   * 2019-09-20T20:20
   * }}}
   *
   * @param str a string in ISO 8601.
   * @return the corresponding [[java.time.LocalDateTime]].
   */
  def strToLocalDateTime(str: String): LocalDateTime = {
    ZonedDateTime.parse(str).toLocalDateTime
  }

  /**
   * Convert a [[java.time.LocalDateTime]] to the number of milliseconds from the epoch of 1970-01-01T00:00:00Z.
   *
   * The time-zone offset from UTC used is [[java.time.ZoneOffset.UTC]].
   *
   * @param localDateTime a [[java.time.LocalDateTime]].
   * @return the corresponding number of milliseconds from the epoch of 1970-01-01T00:00:00Z.
   */
  def localDateTimeToEpochMilli(localDateTime: LocalDateTime): Long = {
    localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli
  }

  /**
   * Convert a [[java.util.Date]] to a [[java.time.LocalDateTime]].
   *
   * The time-zone offset from UTC used is [[java.time.ZoneOffset.UTC]].
   *
   * @param date a [[java.util.Date]].
   * @return the corresponding [[java.time.LocalDateTime]].
   */
  def dateToLocalDateTime(date: Date): LocalDateTime = {
    LocalDateTime.ofInstant(date.toInstant, ZoneOffset.UTC);
  }

  /**
   * Convert a [[java.time.LocalDateTime]] to a [[java.sql.Timestamp]].
   *
   * The time-zone offset from UTC used is [[java.time.ZoneOffset.UTC]].
   *
   * @param localDateTime a [[java.time.LocalDateTime]].
   * @return the corresponding [[java.sql.Timestamp]].
   */
  def localDateTimetoTimestamp(localDateTime: LocalDateTime): Timestamp = {
    Timestamp.from(localDateTime.atOffset(ZoneOffset.UTC).toInstant)
  }

  /**
   * Convert a [[java.sql.Timestamp]] to a [[java.time.LocalDateTime]].
   *
   * @param timestamp a [[java.sql.Timestamp]].
   * @return the corresponding [[java.time.LocalDateTime]].
   */
  def timestampToLocalDateTime(timestamp: Timestamp): LocalDateTime = {
    timestamp.toLocalDateTime
  }
}
