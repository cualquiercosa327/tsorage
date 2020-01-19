package be.cetic.tsorage.processor.aggregator.followup

package object tdouble
{
   val simpleFollowUpDerivators: List[SimpleFollowUpDerivator] = List(
      FollowUpDoubleMax,
      FollowUpDoubleMin,
      FollowUpDoubleSum
   )
}
