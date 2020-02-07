package be.cetic.tsorage.ingestion.message

import be.cetic.tsorage.common.json.MessageJsonSupport


trait TSMessageJsonSupport extends MessageJsonSupport
{
   implicit val datadogMessageFormat = jsonFormat1(TSBody)
}
