package be.cetic.tsorage.processor.aggregator.raw

/**
 * Raw derivators for the tdouble type.
 */
package object tdouble
{
   val simpleRawDerivators: List[SimpleRawAggregator] = List(
      TDoubleMax,
      TDoubleMin,
      TDoubleSum,
      FirstDouble,
      LastDouble
   )
}
