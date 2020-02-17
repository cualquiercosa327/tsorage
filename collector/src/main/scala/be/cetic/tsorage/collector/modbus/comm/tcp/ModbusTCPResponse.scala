package be.cetic.tsorage.collector.modbus.comm.tcp

import be.cetic.tsorage.collector.modbus.ModbusFunction

abstract class ModbusTCPResponse(val transactionId: Int, val unitId: Int, val function: ModbusFunction)
