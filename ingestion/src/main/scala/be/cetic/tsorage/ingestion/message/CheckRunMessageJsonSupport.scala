package be.cetic.tsorage.ingestion.message

import be.cetic.tsorage.common.json.MessageJsonSupport


trait CheckRunMessageJsonSupport extends MessageJsonSupport
{
   implicit val checkRunMessageFormat = jsonFormat6(CheckRunMessage)
}
