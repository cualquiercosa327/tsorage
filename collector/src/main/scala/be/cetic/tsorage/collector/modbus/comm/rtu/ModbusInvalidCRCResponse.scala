package be.cetic.tsorage.collector.modbus.comm.rtu

import be.cetic.tsorage.collector.modbus.ModbusFunction

/**
 * A RTU response with invalid CRC
 */
class ModbusInvalidCRCResponse(unitId: Int, function: ModbusFunction, payload: Array[Byte]) extends ModbusRTUResponse(unitId, function)
