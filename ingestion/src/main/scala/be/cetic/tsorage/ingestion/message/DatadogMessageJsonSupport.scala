package be.cetic.tsorage.ingestion.message

import be.cetic.tsorage.common.json.MessageJsonSupport

trait DatadogMessageJsonSupport extends MessageJsonSupport
{
   implicit val datadogMessageFormat = jsonFormat6(DatadogMessage)
   implicit val datadogBodyFormat = jsonFormat1(DatadogBody)
}
