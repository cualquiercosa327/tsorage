**********************
Technical Requirements
**********************

Sizing
======

.. TODO https://www.ecyrd.com/cassandracalculator/
.. TODO https://robin.io/blog/lessons-learned-cassandra/

.. TODO Also take into account the fact that the gateway and processing node may be replicated for more processing capabilities.

Infrastructure
==============

Software Setting
================


.. _INSTALLATION_CHOOSE_SHARDING:

Choose a Sharding
-----------------

In TSorage, time series are stored using a Cassandra_ database. TSorage takes advantage of Cassandra's wide partitions for
efficiently store data points. Basically, that means all the data points of a time series will be stored in the same partition
(sometimes improperly named a row), in chronological order. Because a Cassandra partition can not grow indefinitely, time series
are partitioned into *shards*, each shard covering a time period of the time series history.

The duration of a shard must be set during the installation, and should never be modified afterwards. All time series have
the same shard duration. To ensure good performances when writing and reading time series, the number of data points
covered by a shard should be limited to a few millions.

A sharding period of one month is often a good choice, since it leads to shards with approximately 45 000 data points
or 2 700 000 data points for typical recording frequencies (one point per minute and per second, respectively).
For higher frequencies, a sharding period of one day may be preferred, which leads to 86 400 000 data points per shard
for a recording frequency of 1 KHz.

.. _Cassandra: http://cassandra.apache.org/

On Premise Deployment
---------------------

.. TODO Talk about 'Cassandra antipatterns'_:
.. TODO If Virtualisation (instead of bare metal): hyperconvergence_ ?

Cloud Deployment
----------------

