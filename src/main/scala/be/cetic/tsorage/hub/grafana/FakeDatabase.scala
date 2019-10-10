package be.cetic.tsorage.hub.grafana

class FakeDatabase(
                    val startTime: Int = (System.currentTimeMillis / 1000).toInt - (24 * 3600) // 24 hours ago.
                  ) extends Database {

  // Bad programming but it is just to perform some tests.

  case class Data(time: Int, temperature: Int, pressure: Int, humidity: Int)

  // Name of attributes.
  val attributes: Seq[String] = Seq("time", "temperature", "pressure", "humidity")

  // Metric names.
  val metrics: Seq[String] = Seq("temperature", "pressure", "humidity")

  // Construct the database.
  val random = new scala.util.Random(42)
  val step = 5 // 5 seconds.
  val data: List[Data] = (for (i <- 1 to (60 * 60 * 24) / step)
    yield {
      val temperature = 10 + random.nextInt((32 - 10) + 1)
      val pressure = 50 + random.nextInt((110 - 50) + 1)
      val humidity = random.nextInt(100 + 1)
      Data(startTime + (step * i), temperature, pressure, humidity)
    }
    ).toList

  // Timestamp in seconds.
  // Return a dictionary where keys are timestamp and values are data.
  def extractData(metricName: String, timestampFrom: Int, timestampTo: Int): List[(Int, Int)] = {
    var dataExtracted = List[(Int, Int)]()

    dataExtracted = for (singleData <- data if (timestampFrom <= singleData.time && singleData.time <= timestampTo))
      yield (
        singleData.time,
        singleData.getClass.getMethod(metricName).invoke(singleData).asInstanceOf[Int] // Bruhh, this is ugly.
      )

    dataExtracted
  }
}
