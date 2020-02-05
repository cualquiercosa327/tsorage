package be.cetic.tsorage.collector.modbus

import akka.util.ByteString
import be.cetic.tsorage.common.messaging.Message

abstract class ModbusResponse(val transactionId: Int, val unitId: Int, val fc: Int)
