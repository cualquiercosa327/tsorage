package be.cetic.tsorage.collector.modbus.comm

trait ModbusValidResponse
{
   def unitId: Int
   def data: Array[Byte]
}
