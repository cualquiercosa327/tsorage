********
Features
********

Tagging
=======

:ref:`Tags <INTRO_CONCEPTS_TAGS>` are arbitrary pairs of `(key, value)` that are used for labelling individual or groups of observations. Systematically labelling improves the semantic knowledge about the stored observations, which facilitates their manual and automatic manipulation. Instead of using a hardly enforceable nomenclature for specifying the meaning of a metric, we recommend to use almost meaningless (from a human being point of view) metric names, and to use tags for specifying its semantics.

Automatic Rollup
================

Rollups consist in aggregating the data points of a time series corresponding to a specific time period. A rollup facilitates
the manipulation of large time series while absolving the user from calculating potentially complex operations on raw data points.

For instance, plotting ten years worth of history, for a time series having one data point per minute, means extracting
and displaying more than 5 millions data points, which represents unreasonable processing costs and latencies.

In the :ref:`Archi_ProcessingLayer`, incoming data points are processed at real time for systematically calculating and storing
aggregated values.

.. _Feature_Derivators:

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
``sum`` divided by `count``. The variance of a time series can be calculated from its ``s_sum``.

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


Arbitrary Recording Frequency
=============================

TSorage supports timestamped observation with a millisecond resolution. If multiple observations coming from the same
time series are ingested with the same timestamp, only one of them will be persisted.

No recording frequency is prescribed. No periodic ingestion is expected among the observations of a time series, nor
between distinct time series.


Unbounded Ingestion Window
==========================

A limitation of most time series databases (including industrial solutions for data historisation) is the existence of an ingestion window. Any incoming measurement having a timestamp preceding the window limit will be ignored. One of the main reasons for this limitation is the fact it allows a definitive compression of the *cold* data points, using a more efficient approach than the one adopted for storing *hot* data points.

When existing, the size of the ingestion window can generally be tuned in order to dynamics the specificities of the monitored system.

While such an ingestion window can make sense in some cases, its size always represents a tradeoff between various technical constraints that are meaningless from a business or operational point of view. It prevents from data ingestion from remote data sources with only occasional access to the storage solution, and makes the ingestion of historical data sets quite challenging.

By default, TSorage uses an unbounded ingestion window, and accepts arbitrarily late observations. Data files receiving late data points are automatically reorganized in order to always store them efficiently. For :ref:`archiving <ARCHIVING>` or advanced analytics purposes, an even more efficient, offline, representation of the time series can be exported from the time series database.


Archiving
=========


Querying
========

Time Series Querying
--------------------


.. _Feature_DataQuerying:

Data Querying
-------------


Real Time Monitoring
====================

Data Quality Monitoring
-----------------------


Alerting
========

