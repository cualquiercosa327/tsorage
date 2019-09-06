package be.cetic.tsorage.processor.source

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Source
import be.cetic.tsorage.processor.FloatMessage

import scala.util.Random

object RandomMessageIterator extends Iterator[FloatMessage] {
  private val tagNames = List("status", "owner", "biz", "test O'test", "~bar")
  private val tagValues = List("hello", "there", "Clara O'toll", "~foo", "Ahaha \" !")

  override def hasNext() = true

  override def next(): FloatMessage = FloatMessage(
    "my sensor",
    Map(
      "status" -> "ok",
      "owner" -> "myself"
    ),
    List((LocalDateTime.now, Random.nextFloat())))

  def source(): Source[FloatMessage, NotUsed] = Source(RandomMessageIterable)

}

object RandomMessageIterable extends scala.collection.immutable.Iterable[FloatMessage] {
  override def iterator: Iterator[FloatMessage] = RandomMessageIterator
}