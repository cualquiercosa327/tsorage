package be.cetic.tsorage.collector.modbus


/**
 * An error response may contain an exception code for further investigation.
 * cf Acromag's  Introduction to modbus TPC/IP, p. 24.
 */
sealed trait ModbusException

object IllegalFunction extends ModbusException     // 01
object IllegalDataAddress extends ModbusException  // 02
object IllegalDataValue extends ModbusException    // 03
object SlaveDeviceFailure extends ModbusException  // 04
object Acknowledge extends ModbusException         // 05
object SlaveDeviceBusy extends ModbusException     // 06
object NegativeAck extends ModbusException         // 07
object MemoryParityError extends ModbusException   // 08
object GatewayPathProblem extends ModbusException  // 0A
object GatewayTargetProblem extends ModbusException// 0B
object ExtendedException extends ModbusException   // FF