package be.cetic.tsorage.ingestion.http

/**
 * A main for the DD HTTP Interface
 */
object DDHTTPMain
{
   def main(args: Array[String]): Unit =
   {
      val interface = new DDHTTPInterface()
      interface.run()
   }
}
