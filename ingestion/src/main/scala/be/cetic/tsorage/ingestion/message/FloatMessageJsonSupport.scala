package be.cetic.tsorage.ingestion.message

import be.cetic.tsorage.common.json.MessageJsonSupport

trait FloatMessageJsonSupport extends MessageJsonSupport
{
   implicit val messageFormat = jsonFormat6(DoubleMessage)
   implicit val bodyFormat = jsonFormat1(DoubleBody)
   implicit val checkRunMessageFormat = jsonFormat6(CheckRunMessage)
}
