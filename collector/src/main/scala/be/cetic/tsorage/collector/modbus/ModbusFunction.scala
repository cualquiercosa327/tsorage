package be.cetic.tsorage.collector.modbus

import com.typesafe.config.Config

sealed abstract class ModbusFunction(val code: Int, val extractName: String)
{
   /**
    * Converts extracts for this function into Modbus requests for the same function.
    *
    * The mapping one extract = one request is not warranted, since some requests can be
    * created for covering multiple extracts.
    *
    * @param unitId     The id of the unit to which the requests will be submitted.
    * @param extracts   The requested extracts
    * @return           A list of requests, the responses to which cover the extracts.
    */
   def prepareRequests(unitId: Int, extracts: List[Extract]): List[ModbusRequest]
}

object ReadCoils extends ModbusFunction(1, "output_coils")
{
   /**
    *
    * @param unitId   The id of the unit to which the requests will be submitted.
    * @param extracts The requested extracts
    * @return
    */
   override def prepareRequests(unitId: Int, extracts: List[Extract]): List[ModbusRequest] =
   {
      extracts.map(extract =>
         new ReadCoilsRequest(
            unitId,
            extract.address,
            typeToRegisterNumber(extract.`type`)
         )
      )
   }
}

object ReadDiscreteInput extends ModbusFunction(2, "input_contacts")
{
   override def prepareRequests(unitId: Int, extracts: List[Extract]): List[ModbusRequest] =
   {
      extracts.map(extract =>
         new ReadDiscreteInputRequest(
            unitId,
            extract.address,
            typeToRegisterNumber(extract.`type`)
         )
      )
   }
}

object ReadHoldingRegister extends ModbusFunction(3, "holding_registers")
{
   override def prepareRequests(unitId: Int, extracts: List[Extract]): List[ModbusRequest] =
   {
      extracts.map(extract =>
         new ReadHoldingRegisterRequest(
            unitId,
            extract.address,
            typeToRegisterNumber(extract.`type`)
         )
      )
   }
}

object ReadInputRegister extends ModbusFunction(4, "input_registers")
{
   override def prepareRequests(unitId: Int, extracts: List[Extract]): List[ModbusRequest] =
   {
      extracts.map(extract =>
         new ReadInputRegisterRequest(
            unitId,
            extract.address,
            typeToRegisterNumber(extract.`type`)
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