package be.cetic.tsorage.hub.filter

import be.cetic.tsorage.common.{Cassandra, FutureManager, TimeSeries}
import be.cetic.tsorage.hub.CandidateTimeSeries
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import collection.JavaConverters._

/**
 * Services for managing time series.
 */
case class TimeSeriesManager(cassandra: Cassandra, conf: Config) extends LazyLogging with FutureManager
{
   private val keyspace = conf.getString("cassandra.keyspaces.other")

   private val session = cassandra.session

   private val metricManager = MetricManager(cassandra, conf)

   /**
    * The time series associated with an existing dynamic tagname.
    */
   private val timeSeriesWithDynamicTagnameStatement = session.prepare(
      QueryBuilder
         .select("metric", "tagset")
         .from(keyspace, "reverse_dynamic_tagset")
         .where(QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname")))
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   private val shardedTimeSeriesWithDynamicTagnameStatement = session.prepare(
      QueryBuilder
         .select("metric", "tagset")
         .from(keyspace, "reverse_sharded_dynamic_tagset")
         .where(QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname")))
         .and(QueryBuilder.eq("shard", QueryBuilder.bindMarker("shard")))
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   private val timeSeriesWithDynamicTagStatement = session.prepare(
      QueryBuilder
         .select("metric", "tagset")
         .from(keyspace, "reverse_dynamic_tagset")
         .where(QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname")))
         .and(QueryBuilder.eq("tagvalue", QueryBuilder.bindMarker("tagvalue")))
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   private val shardedTimeSeriesWithDynamicTagStatement = session.prepare(
      QueryBuilder
         .select("metric", "tagset")
         .from(keyspace, "reverse_sharded_dynamic_tagset")
         .where(QueryBuilder.eq("tagname", QueryBuilder.bindMarker("tagname")))
         .and(QueryBuilder.eq("tagvalue", QueryBuilder.bindMarker("tagvalue")))
         .and(QueryBuilder.eq("shard", QueryBuilder.bindMarker("shard")))
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   private val allTimeSeriesStatement = session.prepare(
      QueryBuilder
         .select("metric", "tagset")
         .from(keyspace, "dynamic_tagset")
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   private val shardedAllTimeSeriesStatement = session.prepare(
      QueryBuilder
         .select("metric", "tagset")
         .from(keyspace, "sharded_dynamic_tagset")
   ).setConsistencyLevel(ConsistencyLevel.ONE)

   /**
    * @param tagname    The name of a tag
    * @param tagvalue   The value of a tag
    * @return           The time series having this dynamic tag.
    */
   private def getTimeSeriesWithDynamicTag(tagname: String, tagvalue: String): Set[TimeSeries] =
   {
      session.execute(
         timeSeriesWithDynamicTagStatement
            .bind()
            .setString("tagname", tagname)
            .setString("tagvalue", tagvalue)
      ).asScala
         .map(row => TimeSeries(row.getString("metric"), row.getMap("tagset", classOf[String], classOf[String]).asScala.toMap))
         .toSet
   }

   /**
    * @param tagname    The name of a tag
    * @param tagvalue   The value of a tag
    * @param shard      A time shard
    * @return           The time series having this dynamic tag in the considered shard
    */
   private def getTimeSeriesWithDynamicTag(tagname: String, tagvalue: String, shard: String): Set[TimeSeries] =
   {
      session.execute(
         shardedTimeSeriesWithDynamicTagStatement
            .bind()
            .setString("tagname", tagname)
            .setString("tagvalue", tagvalue)
            .setString("shard", shard)
      ).asScala
         .map(row => TimeSeries(row.getString("metric"), row.getMap("tagset", classOf[String], classOf[String]).asScala.toMap))
         .toSet
   }

   private def getTimeSeriesWithDynamicTagname(tagname: String): Set[TimeSeries] =
   {
      session.execute(
         timeSeriesWithDynamicTagnameStatement
            .bind()
            .setString("tagname", tagname)
      ).asScala
         .map(row => TimeSeries(row.getString("metric"), row.getMap("tagset", classOf[String], classOf[String]).asScala.toMap))
         .toSet
   }

   private def getTimeSeriesWithDynamicTagname(tagname: String, shard: String): Set[TimeSeries] =
   {
      session.execute(
         shardedTimeSeriesWithDynamicTagnameStatement
            .bind()
            .setString("tagname", tagname)
            .setString("shard", shard)
      ).asScala
         .map(row => TimeSeries(row.getString("metric"), row.getMap("tagset", classOf[String], classOf[String]).asScala.toMap))
         .toSet
   }

   private def getTimeSeriesWithTagname(tagname: String): Set[TimeSeries] =
   {
      val byMetric = metricManager
         .getMetricWithStaticTagname(tagname)
         .flatMap(metric => metric.getTimeSeries())

      val byDynamic = getTimeSeriesWithDynamicTagname(tagname)

      byMetric union byDynamic
   }

   private def getTimeSeriesWithTagname(tagname: String, shard: String): Set[TimeSeries] =
   {
      val byMetric = metricManager
         .getMetricWithStaticTagname(tagname)
         .flatMap(metric => metric.getTimeSeries(shard))

      val byDynamic = getTimeSeriesWithDynamicTagname(tagname, shard)

      byMetric union byDynamic
   }

   private def getTimeSeriesWithTag(tagname: String, tagvalue: String): Set[TimeSeries] =
   {
      val byMetric = metricManager
         .getMetricWithStaticTag(tagname, tagvalue)
         .flatMap(metric => metric.getTimeSeries())

      val byDynamic = getTimeSeriesWithDynamicTag(tagname, tagvalue)

      byMetric union byDynamic
   }

   private def getTimeSeriesWithTag(tagname: String, tagvalue: String, shard: String): Set[TimeSeries] =
   {
      val byMetric = metricManager
         .getMetricWithStaticTag(tagname, tagvalue)
         .flatMap(metric => metric.getTimeSeries(shard))

      val byDynamic = getTimeSeriesWithDynamicTag(tagname, tagvalue, shard)

      byMetric union byDynamic
   }

   /**
    * @param query
    * @return
    */
   def getCandidateTimeSeries(query: TimeSeriesQuery): Set[CandidateTimeSeries] = query.timeRange match
   {
      case None => getCandidateTimeSeries(query.metric, query.filter)
      case Some(timeRange) =>
         cassandra.sharder
            .shards(timeRange.start, timeRange.end).map(shard => getCandidateTimeSeries(query.metric, query.filter, shard))
            .reduce(_ union _)
   }

   /**
    * Main query method.
    * @param query
    * @return
    */
   def getTimeSeries(query: TimeSeriesQuery): Set[TimeSeries] = getCandidateTimeSeries(query).map(c => c.toTimeSeries)


   private def toCandidateTimeSeries(timeseries: Set[TimeSeries]): Set[CandidateTimeSeries] =
   {
      timeseries
         .groupBy(_.metric)
         .flatMap(entry => {
            val staticTagset = Metric(entry._1, session, conf).getStaticTagset()
            entry._2.map(ts => CandidateTimeSeries(entry._1, staticTagset, ts.tagset))
         })
         .toSet
   }

   private def getCandidateTimeSeries(metric: Option[String], filter: Filter): Set[CandidateTimeSeries] = metric match
   {
      case Some(m) =>
         toCandidateTimeSeries(Metric(m, session, conf).getTimeSeries())
            .filter(c => c.accept(filter))
      case None => eval(filter, None).candidates
   }

   private def getCandidateTimeSeries(metric: Option[String], filter: Filter, shard: String): Set[CandidateTimeSeries] = metric match
   {
      case Some(m) => toCandidateTimeSeries(Metric(m, session, conf).getTimeSeries(shard))
         .filter(c => c.accept(filter))
      case None => eval(filter, Some(shard)).candidates
   }

   /**
    * @return All the available time series.
    */
   private def getAllTimeSeries(): Set[TimeSeries] =
   {
      session
         .execute(allTimeSeriesStatement.bind())
         .asScala
         .map(row => TimeSeries(row.getString("metric"), row.getMap("tagset", classOf[String], classOf[String]).asScala.toMap))
         .toSet
   }

   /**
    * @param shard
    * @return All the time series available in a shard.
    */
   private def getAllTimeSeries(shard: String): Set[TimeSeries] =
   {
      session
         .execute(
            shardedAllTimeSeriesStatement.bind()
               .setString("shard", shard)
         )
         .asScala
         .map(row => TimeSeries(row.getString("metric"), row.getMap("tagset", classOf[String], classOf[String]).asScala.toMap))
         .toSet
   }

   /**
    * Replaces pieces of the filter by its evaluation as a set of candidate time series.
    * @param f A filter
    */
   private def eval(f: Filter, shard: Option[String]): EvaluatedFilter = f match
   {
      // Base
      case TagFilter(name, value) => shard match
      {
         case None => EvaluatedFilter(toCandidateTimeSeries(getTimeSeriesWithTag(name, value)))
         case Some(s) => EvaluatedFilter(toCandidateTimeSeries(getTimeSeriesWithTag(name, value, s)))
      }

      case TagExist(tagname) => shard match
      {
         case None => EvaluatedFilter(toCandidateTimeSeries(getTimeSeriesWithTagname(tagname)))
         case Some(s) => EvaluatedFilter(toCandidateTimeSeries(getTimeSeriesWithTagname(tagname, s)))
      }

      case AllFilter => shard match
      {
         case None => EvaluatedFilter(toCandidateTimeSeries(getAllTimeSeries()))
         case Some(s) => EvaluatedFilter(toCandidateTimeSeries(getAllTimeSeries(s)))
      }

      case Not(AllFilter) => EvaluatedFilter(Set.empty)

      // Simplify double negations
      case Not(Not(x)) => eval(x, shard)
      case And(Not(Not(a)), b) => eval(And(a, b), shard)
      case And(a, Not(Not(b))) => eval(And(a, b), shard)
      case Or(Not(Not(a)), b) => eval(Or(a, b), shard)
      case Or(a, Not(Not(b))) => eval(Or(a, b), shard)

      // And
      case And(a: TagFilter, b) => eval(a, shard) filter b
      case And(a: TagExist, b) => eval(a, shard) filter b

      case And(a, b: TagFilter) => eval(b, shard) filter a
      case And(a, b: TagExist) => eval(b, shard) filter a

      // And -- Avoid an explicit evaluation of a negation
      case And(a, b: Not) => eval(a, shard) filter b
      case And(a: Not, b) => eval(b, shard) filter a

      // And -- Avoid evaluating a OR, since it leads to a double evaluation, while a AND could be simplified.
      case And(a, b: Or) => eval(a, shard) filter b
      case And(a: Or, b) => eval(b, shard) filter a

      // And -- a "all" filter has no effect
      case And(a, AllFilter) => eval(a, shard)
      case And(AllFilter, b) => eval(b, shard)

      // And -- General case. For instance, And(And(a,b), And(c,d)).
      // Arbitrary eval the left branch, which could turn out to not be the best decision
      case And(a, b)  => eval(a, shard) filter b

      // Or -- A "all" filter means all time series must be retrieved
      case Or(_, AllFilter) => eval(AllFilter, shard)
      case Or(AllFilter, _) => eval(AllFilter, shard)

      // Or -- General case
      case Or(a, b) => eval(a, shard) union eval(b, shard)

      // Not -- De Morgan
      case Not(And(a, b)) => eval(Or(Not(a), Not(b)), shard)
      case Not(Or(a, b)) => eval(And(Not(a), Not(b)), shard)

      // Worst situation: one has to retrieves the entire time series collection, minus the specific filter
      case n: Not => eval(And(AllFilter, n), shard)
   }
}
