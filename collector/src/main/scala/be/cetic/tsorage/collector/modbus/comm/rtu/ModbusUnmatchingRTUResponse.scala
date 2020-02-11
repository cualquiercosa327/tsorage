package be.cetic.tsorage.collector.modbus.comm.rtu

import be.cetic.tsorage.collector.modbus.ModbusFunction
import be.cetic.tsorage.collector.modbus.comm.ModbusRequest

/**
 * A RTU response that does not correspond to the current request.
 */
class ModbusUnmatchingRTUResponse(
                                    unitId: Int,
                                    function: ModbusFunction,
                                    val currentRequest: Option[ModbusRequest])
   extends ModbusRTUResponse(unitId, function)
{


}
