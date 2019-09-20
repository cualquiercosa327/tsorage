package be.cetic.tsorage.processor.source

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Source
import be.cetic.tsorage.processor.Message

import scala.util.Random

object RandomMessageIterator extends Iterator[Message[Double]] {
  private val tagNames = List("status", "owner", "biz", "test O'test", "~bar")
  private val tagValues = List("hello", "there", "Clara O'toll", "~foo", "Ahaha \" !")

  override def hasNext() = true

  override def next(): Message[Double] = Message(
    "my sensor",
    Map(
      "status" -> "ok",
      "owner" -> "myself"
    ),
    List((LocalDateTime.now, Random.nextFloat())))

  def source(): Source[Message[Double], NotUsed] = Source(RandomMessageIterable)

}

object RandomMessageIterable extends scala.collection.immutable.Iterable[Message[Double]] {
  override def iterator: Iterator[Message[Double]] = RandomMessageIterator
}