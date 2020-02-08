package be.cetic.tsorage.collector.modbus.comm.rtu

import be.cetic.tsorage.collector.modbus.ModbusFunction

abstract class ModbusRTUResponse(val unitId: Int, val function: ModbusFunction)

