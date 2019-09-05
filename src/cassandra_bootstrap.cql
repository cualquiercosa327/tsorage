CREATE  KEYSPACE IF NOT EXISTS tsorage_agg
    WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }
    AND DURABLE_WRITES = true;

CREATE  KEYSPACE IF NOT EXISTS tsorage_raw
    WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }
    AND DURABLE_WRITES = true;

CREATE TABLE tsorage_agg.numeric (
                                     metric text,
                                     shard text,
                                     interval text,
                                     aggregator text,
                                     datetime timestamp,
                                     value float,
                                     observation_datetime timestamp,
                                     PRIMARY KEY ((metric, shard, interval, aggregator), datetime)
) WITH CLUSTERING ORDER BY (datetime DESC);

CREATE TABLE tsorage_raw.numeric (
                                     metric text,
                                     shard text,
                                     datetime timestamp,
                                     value float,
                                     PRIMARY KEY ((metric, shard), datetime)
) WITH CLUSTERING ORDER BY (datetime DESC);