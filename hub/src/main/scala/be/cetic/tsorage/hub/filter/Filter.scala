package be.cetic.tsorage.hub.filter

sealed trait Filter
{
   /**
    * @return The names of the tags explicitly involved in the filter.
    */
   def involvedTagNames: Set[String]
}

/**
 * A filter representing the fact that a tag must exist with a particular name and a particular value
 * @param name    The name of the filtering tag.
 * @param value   The value of the filtering tag
 */
case class TagFilter(name: String, value: String) extends Filter
{
   override def involvedTagNames: Set[String] = Set(name)
}

/**
 * A filter based on the fact that a tag having a particular name exists.
 * @param name The name of the tag that must exist.
 */
case class TagExist(name: String) extends Filter
{
   override def involvedTagNames: Set[String] = Set(name)
}

/**
 * A filter inverting the effect of an underlying filter.
 * @param filter The original filter, the effect of which will be inverted.
 */
case class Not(filter: Filter) extends Filter
{
   override def involvedTagNames: Set[String] = filter.involvedTagNames
}

/**
 * Combines two filters by applying a logical AND between them.
 * @param a A filter.
 * @param b Another filter
 */
case class And(a: Filter, b: Filter) extends Filter
{
   override def involvedTagNames: Set[String] = a.involvedTagNames union b.involvedTagNames
}

/**
 * Combines two filters by applying a logical OR between them.
 * @param a A filter.
 * @param b Another filter
 */
case class Or(a: Filter, b: Filter) extends Filter
{
   override def involvedTagNames: Set[String] = a.involvedTagNames union b.involvedTagNames
}