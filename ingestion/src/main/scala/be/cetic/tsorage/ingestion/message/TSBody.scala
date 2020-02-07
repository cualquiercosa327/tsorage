package be.cetic.tsorage.ingestion.message

import be.cetic.tsorage.common.messaging.Message

case class TSBody(series: List[Message])
