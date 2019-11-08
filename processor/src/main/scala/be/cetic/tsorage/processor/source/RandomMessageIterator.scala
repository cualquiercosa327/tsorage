package be.cetic.tsorage.processor.source

import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Source
import be.cetic.tsorage.processor.Message
import spray.json.JsNumber

import scala.util.Random

object RandomMessageIterator extends Iterator[Message] {
  private val tagNames = List("status", "owner", "biz", "test O'test", "~bar")
  private val tagValues = List("hello", "there", "Clara O'toll", "~foo", "Ahaha \" !")

  override def hasNext() = true

  override def next(): Message = Message(
    "my sensor",
    Map(
      "status" -> "ok",
      "owner" -> "myself"
    ),
    "tdouble",
    List((LocalDateTime.now, JsNumber(Random.nextDouble())))
  )

  def source(): Source[Message, NotUsed] = Source(RandomMessageIterable)

}

object RandomMessageIterable extends scala.collection.immutable.Iterable[Message] {
  override def iterator: Iterator[Message] = RandomMessageIterator
}