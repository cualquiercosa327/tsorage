package be.cetic.tsorage.collector.modbus

sealed abstract class ModbusFunction(val code: Int, extractName: String)

object ReadCoilsValid extends ModbusFunction(1, "output_coils")
object ReadDiscreteInput extends ModbusFunction(2, "input_contacts")
object ReadHoldingRegister extends ModbusFunction(3, "holding_registers")
object ReadInputRegister extends ModbusFunction(4, "input_registers")