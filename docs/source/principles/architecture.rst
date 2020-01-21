************
Architecture
************

Overview
========

The TSorage project is based on a modular architecture, with all modules being designed to be executed in distinct Docker containers.
This makes TSorage a portable solution, with simple and standardized deployment steps. It also offers the possibility
to place the components on different physical and virtual machines, making it available on a wide range of platforms and services.
Furthermore, (re)sizing a containerized architecture is easier, since a component can be moved to a platform offering more resources.
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

More details about the message format are available in the :ref:`IngestingTimeSeries` section.

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



Storage
=======

Hub Services
============

Authentication
--------------

Tag Management
--------------