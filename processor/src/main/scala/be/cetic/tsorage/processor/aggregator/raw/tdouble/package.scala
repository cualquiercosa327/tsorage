package be.cetic.tsorage.processor.aggregator.raw

import be.cetic.tsorage.processor.datatype.DoubleSupport

/**
 * Raw derivators for the tdouble type.
 */
package object tdouble
{
   val simpleRawDerivators: List[SimpleRawDerivator] = List(
      TDoubleMax,
      TDoubleMin,
      TDoubleSum,
      TDoubleSSum
   )
}
