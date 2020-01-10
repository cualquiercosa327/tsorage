package be.cetic.tsorage.hub

import be.cetic.tsorage.common.TimeSeries
import be.cetic.tsorage.hub.filter._

/**
 * A time series with its static tagset.
 */
case class CandidateTimeSeries(metric: String, staticTagset: Map[String, String], dynamicTagset: Map[String, String])
{
   /**
    * Determines whether the candidate time series contains a given filter
    * @param f The predicate
    * @return true if the specified tagname exists in the candidate time series, false otherwise.
    */
   def accept(f: Filter): Boolean = f match {
      case TagExist(tagname) => (staticTagset contains tagname) || (dynamicTagset contains tagname)

      case TagFilter(tagname, tagvalue) => {
         val staticF = staticTagset.get(tagname).map(_ == tagvalue).getOrElse(false)
         lazy val dynamicF = dynamicTagset.get(tagname).map(_ == tagvalue).getOrElse(false)
         staticF || dynamicF
      }

      case Not(f2: Filter) => ! this.accept(f2)

      case And(a, b) => accept(a) && accept(b)

      case Or(a, b) => accept(a) || accept(b)

      case AllFilter => true
   }

   def toTimeSeries: TimeSeries = TimeSeries(metric, dynamicTagset)

   /**
    * @return  All the tagnames associated with this candidate.
    */
   def tagnames: Set[String] = staticTagset.keySet union dynamicTagset.keySet

   /**
    * @param tagname A tag name.
    * @return  All the values associated with the given tag name.
    */
   def tagvalues(tagname: String): Set[String] = Set(
      staticTagset.get(tagname),
      dynamicTagset.get(tagname)
   ).flatten
}