package be.cetic.tsorage.processor.source

import java.time.{Duration, LocalDateTime}

import akka.NotUsed
import akka.stream.scaladsl.Source
import be.cetic.tsorage.processor.Message
import spray.json.JsNumber

import scala.util.Random

object RandomMessageIterator extends Iterator[Message] {
  private val tagNames = List("status", "owner", "biz", "test O'test", "~bar")
  private val tagValues = List("hello", "there", "Clara O'toll", "~foo", "Ahaha \" !")

  override def hasNext() = true

  private var count = 0;
  private val initTimeStamp = LocalDateTime.now()
  private val value = JsNumber(42)

  override def next(): Message = {
    count = count + 1

    if(count % 1000 == 0)
    {
      val duration = Duration.between(initTimeStamp, LocalDateTime.now())
     // println(s"${count} / ${duration.toSeconds} -> ${count.toLong / (duration.toSeconds)}")
    }

    Message(
      s"my sensor ${Random.nextInt(5000)}",
      Map(
        "status" -> "ok",
        "owner" -> "myself",
        //    tagNames(Random.nextInt(tagNames.size)) -> tagValues(Random.nextInt(tagValues.size))
      ),
      "tdouble",
      (1 to 500).map(iteration => (LocalDateTime.now.withNano(iteration*1000000), value)).toList
    )
  }

  def source(): Source[Message, NotUsed] = Source(RandomMessageIterable)

}

object RandomMessageIterable extends scala.collection.immutable.Iterable[Message] {
  override def iterator: Iterator[Message] = RandomMessageIterator
}