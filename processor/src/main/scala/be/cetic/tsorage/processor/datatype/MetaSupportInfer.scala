package be.cetic.tsorage.processor.datatype


object MetaSupportInfer
{
   def inferSupport(`type`: String) = List(
      DataTypeSupport.availableSupports.get(`type`),
      DatedTypeSupport.availableSupports.get(`type`)
   ).flatten.head
}
