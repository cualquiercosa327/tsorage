package be.cetic.tsorage.hub.filter

import be.cetic.tsorage.common.Cassandra
import com.datastax.driver.core.{ConsistencyLevel, Session}
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import collection.JavaConverters._


/**
 * An entity for managing tag recommendations.
 */
case class TagManager(cassandra: Cassandra, conf: Config) extends LazyLogging
{
   private val keyspace = conf.getString("cassandra.keyspaces.other")

   private lazy val metricManager = MetricManager(cassandra, conf)

   private val session = cassandra.session

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
      val filteredMetrics = metricManager.getMetrics(filter, range)

      val staticTagNames = filteredMetrics.par
         .map(metric => metricManager.getStaticTagset(metric).keySet)
         .reduce( (n1, n2) => n1 union n2)

      val dynamicTagNames = filteredMetrics.par
         .map(metric => metricManager.getDynamicTagset(metric, range).keySet)
         .reduce( (n1, n2) => n1 union n2)

      val tagNames = staticTagNames ++ dynamicTagNames

      // remove names of tags already used in the filter
      tagNames -- filter.involvedTagNames
   }
}
