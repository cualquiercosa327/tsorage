package be.cetic.tsorage.collector.modbus

abstract class ModbusResponse(val transactionId: Int, val unitId: Int, val function: ModbusFunction)
