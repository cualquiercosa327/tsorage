package be.cetic.tsorage.hub.grafana

trait Database {
  val attributes: Seq[String]
  val metrics: Seq[String]
  def extractData(metricName: String, timestampFrom: Int, timestampTo: Int): List[(Int, Int)]
}
