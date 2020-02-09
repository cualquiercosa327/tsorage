package be.cetic.tsorage.collector.modbus

import be.cetic.tsorage.collector.modbus.comm.{ModbusRequest, ReadCoilsRequest, ReadDiscreteInputRequest, ReadHoldingRegisterRequest, ReadInputRegisterRequest}

sealed abstract class ModbusFunction(val code: Int, val extractName: String)
{
   /**
    * Converts extracts for this function into Modbus requests for the same function.
    *
    * The mapping one extract = one request is not warranted, since some requests can be
    * created for covering multiple extracts.
    *
    * @param extracts   The requested extracts
    * @return           A list of requests, the responses to which cover the extracts.
    */
   def prepareRequests(extracts: List[Extract]): List[ModbusRequest]
}

object ReadCoils extends ModbusFunction(1, "output_coils")
{
   /**
    *
    * @param extracts The requested extracts
    * @return
    */
   override def prepareRequests(extracts: List[Extract]): List[ModbusRequest] =
   {
      extracts.map(extract =>
         new ReadCoilsRequest(
            extract.unitId,
            extract.address,
            extract.`type`.registerCount
         )
      )
   }
}

object ReadDiscreteInput extends ModbusFunction(2, "input_contacts")
{
   override def prepareRequests(extracts: List[Extract]): List[ModbusRequest] =
   {
      extracts.map(extract =>
         new ReadDiscreteInputRequest(
            extract.unitId,
            extract.address,
            extract.`type`.registerCount
         )
      )
   }
}

object ReadHoldingRegister extends ModbusFunction(3, "holding_registers")
{
   override def prepareRequests(extracts: List[Extract]): List[ModbusRequest] =
   {
      extracts.map(extract =>
         new ReadHoldingRegisterRequest(
            extract.unitId,
            extract.address,
            extract.`type`.registerCount
         )
      )
   }
}

object ReadInputRegister extends ModbusFunction(4, "input_registers")
{
   override def prepareRequests(extracts: List[Extract]): List[ModbusRequest] =
   {
      extracts.map(extract =>
         new ReadInputRegisterRequest(
            extract.unitId,
            extract.address,
            extract.`type`.registerCount
         )
      )
   }
}

object ModbusFunction
{
   def apply(name: String): ModbusFunction = name match {
      case "output_coils" => ReadCoils
      case "input_contacts" => ReadDiscreteInput
      case "holding_registers" => ReadHoldingRegister
      case "input_registers" => ReadInputRegister
   }

   def apply(code: Int): ModbusFunction = code match {
      case 1 => ReadCoils
      case 2 => ReadDiscreteInput
      case 3 => ReadHoldingRegister
      case 4 => ReadInputRegister
   }
}