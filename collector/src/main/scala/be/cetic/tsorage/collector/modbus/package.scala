package be.cetic.tsorage.collector

package object modbus
{
   private val charRegex = """char(\d+)""".r

   def typeToRegisterNumber(`type`: String): Int =
   {
      `type` match {
         case "bool16" => 1

         case "uint16" => 1
         case "sint16" => 1

         case "uint32" => 2
         case "sint32" => 2

         case "sfloat32" => 2
         case "enum16" => 1

         case charRegex(length) => (length.toInt / 2) + 1
      }
   }
}
