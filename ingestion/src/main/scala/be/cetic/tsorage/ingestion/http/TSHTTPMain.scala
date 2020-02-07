package be.cetic.tsorage.ingestion.http

/**
 * A main for the TS HTTP Interface
 */
object TSHTTPMain
{
   def main(args: Array[String]): Unit =
   {
      val interface = new TSHTTPInterface()
      interface.run()
   }
}

