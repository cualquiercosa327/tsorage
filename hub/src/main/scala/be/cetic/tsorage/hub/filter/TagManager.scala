package be.cetic.tsorage.hub.filter

import be.cetic.tsorage.hub.Cassandra
import be.cetic.tsorage.hub.tag.TagValueQuery
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._


/**
 * An entity for managing tag recommendations.
 */
case class TagManager(cassandra: Cassandra, conf: Config) extends LazyLogging
{
   private val keyspace = conf.getString("cassandra.keyspaces.other")

   private lazy val metricManager = MetricManager(cassandra, conf)
   private lazy val timeSeriesManager = TimeSeriesManager(cassandra, conf)

   private val session = cassandra.session

   private val staticValueQuery = session.prepare(QueryBuilder
      .select("tagvalue")
      .from(keyspace, "reverse_static_tagset")
      .where(QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname")))
   ).setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)

   private val dynamicGlobalValueQuery = session.prepare(
      QueryBuilder
         .select("tagvalue")
         .from(keyspace, "reverse_dynamic_tagset")
         .where(QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname")))
   ).setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)

   private val dynamicShardedValueQuery = session.prepare(
      QueryBuilder.select("tagvalue")
         .from(keyspace, "reverse_sharded_dynamic_tagset")
         .where(QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname")))
         .and(QueryBuilder.eq("shard", QueryBuilder.bindMarker("shard")))
   ).setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)


   /**
    * Retrieves the names of all tags recorded in the system.
    * @param range An optional time range restricting the tag names retrieved for the dynamic tags.
    * @return The name of all dynamic and static tags recorded in the system.
    *         If a range is specified, only dynamic tags from this time range are taken into account, but
    *         the results are approximate since dynamic tags are recorded by shard.
    */
   def getAllTagNames(range: Option[QueryDateRange]): Set[String] =
   {
      val staticTagNames = session.execute(
         QueryBuilder
            .select("tagname")
            .distinct()
            .from(keyspace, "reverse_static_tagset")
            .setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
      ).asScala
       .map(row => row.getString("tagname"))
       .toSet


      val dynamicTagNames = range match
      {
         case None => session.execute(
            QueryBuilder
               .select("tagname")
               .distinct()
               .from(keyspace, "reverse_static_tagset")
               .setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
         ).asScala
          .map(row => row.getString("tagname"))
          .toSet

         case Some(r) =>
         {
            val shards = cassandra.sharder.shards(r.start, r.end)
            shards.par.map(shard => session.execute(
               QueryBuilder.select("tagname")
                  .distinct()
                  .from(keyspace, "reverse_sharded_dynamic_tagname")
                  .where(QueryBuilder.eq("shard", shard))
                  .setConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
            ).asScala
               .map(row => row.getString("tagname"))
               .toSet
            ).reduce((n1, n2) => n1 union n2)
         }
      }

      staticTagNames union dynamicTagNames
   }

   /**
    * Suggests relevant tag names based on an existing tagset.
    *
    * Retrieved tag names associated with metrics corresponding to a given filter..
    * @param filter    A predicate filtering metrics.
    * @param range     An optional time range restriction.
    * @return  Names of tags being associated with the filtered metrics.. Optionally, these tag names may be
    *          restricted to a given time range. In that case, results are approximate, since dynamic tags are
    *          recorded by shard. Please note tagnames that are explicitly mentioned in filter are omitted;
    */
   def suggestTagNames(filter: Filter, range: Option[QueryDateRange]): Set[String] =
   {
      // TODO : allow empty filter, so that all tag names must be retrieved

      val inUse = timeSeriesManager
         .getCandidateTimeSeries(TimeSeriesQuery(None, filter, range))
         .flatMap(c => c.tagnames)

      // remove names of tags already used in the filter
      inUse -- filter.involvedTagNames
   }

   /**
    * @param tagname A tag name
    * @param range An optional time range constraint.
    * @return  The values associated with the given tag name, optionally in the specified time range.
    */
   def getTagValues(tagname: String, range: Option[AbsoluteQueryDateRange]): Set[String] =
   {
      val staticValues = session.execute(
         staticValueQuery.bind().setString("tagname", tagname)
      ).asScala.map(row => row.getString("tagvalue")).toSet

      val dynamicValues = range match {
         case None => session.execute(
            dynamicGlobalValueQuery.bind("tagname", tagname)
         ).asScala.map(row => row.getString("tagvalue")).toSet

         case Some(QueryDateRange(start, end)) => {   // With time range
            val shards = cassandra.sharder.shards(start, end)
            shards.par.map(shard => session.execute(
               dynamicShardedValueQuery.bind()
                  .setString("tagname", tagname)
                  .setString("shard", shard)
            ).asScala
               .map(row => row.getString("tagvalue"))
               .toSet
            ).reduce( (v1, v2) => v1 union v2)
         }
      }

      staticValues union dynamicValues
   }

   /**
    * Suggests values for a specific tag name, corresponding to a selection of metrics.
    * @param context   The context of the suggestion.
    * @return        Relevant values for the specified context
    */
   def suggestTagValues(context: TagValueQuery): Set[String] = context.filter match
   {
      case None => getTagValues(context.tagname, context.range)

      case Some(filter) => {
         val candidates = timeSeriesManager.getCandidateTimeSeries(TimeSeriesQuery(None, filter, context.range))

         candidates.flatMap(c => c.tagvalues(context.tagname))
      }
   }
}
