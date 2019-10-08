package be.cetic.tsorage.hub.grafana

object FakeDatabase {

  // Very bad programming but it is just to perform some tests.

  case class Data(time: Int, temperature: Int, pressure: Int, humidity: Int)

  // Name of attributes.
  val attributes: Seq[String] = Seq("time", "temperature", "pressure", "humidity")

  // Sensor names.
  val sensors: Seq[String] = Seq("temperature", "pressure", "humidity")

  // Construct the database.
  //val currentTime: Int = (System.currentTimeMillis / 1000).toInt
  val currentTime: Int = (System.currentTimeMillis / 1000).toInt - (24 * 3600) // 24 hours ago.
  //val currentTime = 1568991600 // Correspond to Friday 20 September 2019 15:00:00.
  //val currentTime = 1568991734 // Correspond to Friday 20 September 2019 15:02:14.
  /*
  val data: List[Data] = List[Data](
    Data(currentTime, 20, 100, 80),
    Data(currentTime + (3600 * 1), 21, 90, 79), // Add one hours.
    Data(currentTime + (3600 * 2), 22, 88, 76), // Add two hours.
    Data(currentTime + (3600 * 3), 23, 80, 77),  // Add thee hours.
    Data(currentTime + (3600 * 4), 21, 82, 76), // etc.
    Data(currentTime + (3600 * 5), 19, 88, 77),
    Data(currentTime + (3600 * 6), 17, 92, 77),
    Data(currentTime + (3600 * 7), 18, 92, 76),
    Data(currentTime + (3600 * 8), 17, 93, 78),
    Data(currentTime + (3600 * 9), 15, 91, 82),
    Data(currentTime + (3600 * 10), 18, 94, 84),
    Data(currentTime + (3600 * 11), 17, 412, 83),
    Data(currentTime + (3600 * 12), 20, 84, 80),
    Data(currentTime + (3600 * 13), 22, 87, 92),
    Data(currentTime + (3600 * 14), 24, 83, 93),
    Data(currentTime + (3600 * 15), 25, 79, 90),
    Data(currentTime + (3600 * 16), 26, 77, 89),
    Data(currentTime + (3600 * 17), 26, 78, 94),
    Data(currentTime + (3600 * 18), 25, 73, 97),
    Data(currentTime + (3600 * 19), 24, 76, 92),
    Data(currentTime + (3600 * 20), 27, 70, 89),
    Data(currentTime + (3600 * 21), 29, 67, 92),
    Data(currentTime + (3600 * 22), 31, 64, 93),
    Data(currentTime + (3600 * 23), 27, 66, 95),
    Data(currentTime + (3600 * 24), 28, 65, 89)
  )
  */
  ///*
  val random = new scala.util.Random(42)
  val step = 5 // 5 seconds.
  val data: List[Data] = (for (i <- 1 to (60 * 60 * 24) / step)
    yield {
      val temperature = 10 + random.nextInt((32 - 10) + 1)
      val pressure = 50 + random.nextInt((110 - 50) + 1)
      val humidity = random.nextInt(100 + 1)
      Data(currentTime + (step * i),
        temperature,
        pressure,
        humidity)
    }
    ).toList
  //*/

  // Timestamp in seconds.
  // Return a dictionary where keys are timestamp and values are data.
  def extractData(sensorName: String, timestampFrom: Int, timestampTo: Int): List[(Int, Int)] = {
    var dataExtracted = List[(Int, Int)]()

    dataExtracted = for (singleData <- data if (timestampFrom <= singleData.time && singleData.time <= timestampTo))
      yield (
        singleData.time,
        singleData.getClass.getMethod(sensorName).invoke(singleData).asInstanceOf[Int] // Bruhh, this is ugly.
      )

    dataExtracted
  }
}
