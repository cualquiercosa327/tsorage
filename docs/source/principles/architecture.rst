************
Architecture
************

Overview
========

TSorage is based on a modular architecture, where components are running in distinct Docker containers. This makes TSorage
a portable solution, with simple and standardized deployment steps. It also offers the possibility to place the components
on different physical and virtual machines, making it available on a wide range of platforms and services. Furthermore,
(re)sizing a containerized architecture is easier, since a component can be moved to a platform offering more resources.
Under certain conditions, containers can be duplicated in order to increase the performances of the underlying modules.


Collection Layer
================

Various collectors (a.k.a *collection agents*) can be deployed close to a data source for extracting data points from
sensors, databases, spreadsheets, etc. An agent is an active component that periodically polls data sources, and submits
the collected data points to a component of the ingestion layer.

Currently supported or developed collectors are:

- `DataDog Agent`_ : an open source tool developped by DataDog_ for collecting infrastructure- and application-related
metrics.

.. _`DataDog Agent`: https://docs.datadoghq.com/agent/
.. _DataDog: https://www.datadoghq.com/

Ingestion Layer
===============

Components of the ingestion layer are responsible of accepting incoming connexions for ingesting data points into TSorage.
While basic checks (such as submitter authentication) are performed at this point, ingestion components are not supposed
to perform data processing and try to forward the incoming data points as fast as possible. This ensures a highly effective
ingestion, with important throughput rates. Ingestion components can easily be duplicated in order to face occasional or
steady increasing needs for ingestion capacities.

Components expects to receive *messages*. Currently, each message is represented by a JSON document. A message contains

- The metric id, for which new data points are provided.
- The dynamic tagset associated with all the data points described in the message.
- The type of all the data points described in the message. While using the same type of all data point relating to
a metric is generally considered as a good practice, the type associated with a metric can change from a message to
another.
- A collection of timestamped data points having the specified type.

More details about the message format are available in the `Ingesting Time Series`_.

The following components are parts of the ingestion layer:

- A REST API, that provides an HTTP(S) endpoint for submitting new values.
- A MQTT server, that acts like a broker and ingests incoming messages.

All ingestion components push the incoming message to a message queuing system for further processing.


Message Queuing
===============

In TSorage, Kafka is used as a message queuing system for various purposes. Kafka organizes published recourds into *topics*,
each of them being a category or feed name to which records are published. In TSorage, the following topics are
systematically deployed:

- **raw:** for incoming messages.
- **observations:** for the data points represented in messages, or data points derivated by the processing layer.
- **aggregations:** for the aggregated values derivated from data points by the processing layer.

Kafka is an internal component, and its topics should only be accessed by other TSorage components and advanced users.

The purpose of the ``raw`` topic is to persist incoming messages until the Processor layer consumes them.
Consequently, the message queuing component should not be considered as a long-term storage solution for time series data points,
but rather as a buffer that gives processor components the time to process them.

Depending on the resources available on the underlying infrastructure, Kafka can store a few days or weeks worth of
messages on disk, and will automatically delete the oldest ones when running out of space.

Depending on your deployment dependencies, Kafka can be deployed on many nodes, constituting a Kafka cluster. For a
production environment, we recommend to deploy at least 3 Kafka nodes, in order to offer resiliency to failure and
a better workload distribution. The storage capacity of the Kafka cluster can be extended on demand by adding
additional nodes to the cluster.


Processing Layer
================

Derivators
----------

The processing layer consumes messages submitted to the ``raw`` Kafka topic by the ingestion layer in order to populate
the time series database with both raw and temporally aggregated values. A temporal aggregation consists in *summarizing*
multiple observations belonging to the same time period into higher-level values (typically a single one), for a given
time period. In TSorage, operations for temporal aggregations are named *derivators*.

Some built-in derivators are installed by default:

- **all data types:**
    - **count:** Counts the number of data points during the period.
    - **first:** Takes the first data point, in order of time, during the period
    - **last:** Takes the last data point, in order of time, during the period
- **tdouble (real numbers):**
    - **min:** Takes the minimal value observed during the period.
    - **max:** Takes the maximal value observed during the period.
    - **sum:** Takes the sum of all values observed during the period.
    - **s_sum:** Takes the sum of the squared values observed during the period.

From these derivators, higher-level properties can be calculated. For instance, the mean value can be calculated as the
``sum`` divided by `count``. The variance of a time series can be calculated from its ``s_sum```

In the processing layer, business-specific derivators can be specified in order to meet the final user's needs. For the
moment, the only way the specify these derivators is by editing the source code, which may be a bit tedious. Editing
derivators from outside the application is a planned feature for an upcoming version of TSorage.


Time Aggregators
----------------

A part of the configuration file associated with the processing layer describes the successive time periods that must be
considered when performing prepared aggregations. More precisely, a (potentially empty) sequence of time durations
(also known as *time aggregators*) is set in the configuration file, and used by the processing layer every time a
data point is added to the system.

For instance, if the sequence ``[1m,1h]`` is set in the configuration file, raw data points will be converted by buckets
of one minute, then by buckets of one hour.

Currently, the following time aggregators are supported:

- **1m:** one minute
- **1h:** one hour
- **1d:** one day
- **1mo:** one month

Aggregators must be specified by increasing period duration.

Aggregations
------------

The processing layer consumes messages published to the ``raw`` topic, and transforms them into raw observations. These
observations are stored unaltered in the time series database. After that, aggregations are performed according to the
following simplified process:

1. The time period, corresponding to the first time aggregator applied to an added observation, is calculated.
2. All the data points stored in the time series database, that belong to the same time series and have a timestamp
belonging to the calculated time period, are retrieved.
3. The data points are aggregated by applying all the derivators that comply with the data type of the time series.
4. Aggregated values are stored in the time series database, and are further aggregated by applying derivators with the next
time aggregator.
5. Step (4) is repeated until all time aggregators have been used.

Because only composable derivators are carried out, follow-up aggregation can be calculated based on time periods corresponding
to a previous time aggregator, instead of retrieving all the raw data points covering each aggregator. This means less
pressure to the time series database and the processing component.

Storage
=======

Hub Services
============

Authentication
--------------

Tag Management
--------------