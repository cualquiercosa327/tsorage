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

object ModbusException
{
   def apply(code: Byte): ModbusException = code match
   {
      case 0x01 => IllegalFunction
      case 0x02 => IllegalDataAddress
      case 0x03 => IllegalDataValue
      case 0x04 => SlaveDeviceFailure
      case 0x05 => Acknowledge
      case 0x06 => SlaveDeviceBusy
      case 0x07 => NegativeAck
      case 0x08 => MemoryParityError
      case 0x0A => GatewayPathProblem
      case 0x0B => GatewayTargetProblem
      case 0xFF => ExtendedException
   }
}