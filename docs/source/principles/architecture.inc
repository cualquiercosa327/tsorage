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


.. _Archi_CollectionLayer:

Collection Layer
================

Various collectors (a.k.a *collection agents*) can be deployed close to a data source for extracting data points from
sensors, databases, spreadsheets, etc. An agent is an active component that periodically polls data sources, and submits
the collected data points to a component of the ingestion layer.

Currently supported or developed collectors are:

- `DataDog Agent`_ : an open source tool developped by DataDog_ for collecting infrastructure- and application-related metrics.

.. _`DataDog Agent`: https://docs.datadoghq.com/agent/

.. _DataDog: https://www.datadoghq.com/


.. _Archi_IngestionLayer:

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
- The type of all the data points described in the message. While using the same type of all data point relating to a metric is generally considered as a good practice, the type associated with a metric can change from a message to another.
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

The purpose of the ``raw`` topic is to persist incoming messages until the :ref:`Processing layer <Archi_ProcessingLayer>` consumes them.
Consequently, the message queuing component should not be considered as a long-term storage solution for time series data points,
but rather as a buffer that gives processor components the time to process them.

Depending on the resources available on the underlying infrastructure, Kafka can store a few days or weeks worth of
messages on disk, and will automatically delete the oldest ones when running out of space.

Depending on your deployment preferences, Kafka can be deployed on many nodes, constituting a Kafka cluster. For a
production environment, we recommend to deploy at least 3 Kafka nodes, in order to offer resiliency to failure and
a better workload distribution. The storage capacity of the Kafka cluster can be extended on demand by adding
additional nodes to the cluster.

.. _Archi_ProcessingLayer:

Processing Layer
================

The processing layer is instantiated by the ``processor`` component. This component is implemented with `Akka Stream`_,
and provides a modern, reactive, asynchronous, and real-time flow processing.

.. figure:: figures/processor-flow-global.png
   :align: center
   :width: 100 %
   :alt: Global view of Processor Flow

   Global view of the Processor Flow. Each named blox corresponds to a processor graph.

.. figure:: figures/processor-flow-legend.png
   :align: center
   :width: 20 %
   :alt: Processor flow -- message legend

   Message legend


Tag Block
---------

.. figure:: figures/processor-flow-tag.png
   :align: center
   :width: 100 %
   :alt: Process Flow -- Tag Block

   Tag block of the Processor component.

The role of the Tag block is to keep dynamic tags indices up to date as new observations are ingested by the system.
Other processing blocks send it informations about the tagsets mentioned in the ingested messages or inferred during
message processing.


Message Block
-------------

.. figure:: figures/processor-flow-message.png
   :align: center
   :width: 100 %
   :alt: Process Flow -- Message Block

   Message block of the Processor component.

Messages stored in the ``raw`` Kafka topic are consumed by the Message block, which stores the data points they contain.

The :ref:`dynamic tags <Feature_DynamicTag>` mentioned in the messages are submitted to the *Tag block* for further processing.

After extracted data points are processed, they are sent to the *Observation block*, which can transform them into other
observations and calculate aggregated values.

Finally, the Message block performs a first temporal aggregation on the incoming data points, by applying
:ref:`derivators <Feature_Derivators>` on the raw observations belonging to the same time periods than these data points,
according to the first temporal aggregator.

Observation Block
-----------------

.. figure:: figures/processor-flow-observation.png
   :align: center
   :width: 100 %
   :alt: Process Flow -- Observation Block

   Observation block of the Processor component.

The Observation block provides a way to further process observations, either by generating derived observations, or by
calculating temporal aggregations.

.. topic:: Example of Observation Derivation

    A city acquires GPS tracks of the vehicles travelling on its roads, in order to calculate some traffic statistics
    for its urban planning service.

    On the collected data flow, each vehicle periodically emits its GPS coordinates as well as its current speed and other
    real time properties. From the urban planning service point of view, this information is not very useful, so observations
    are derived in order to feed time series that focus on road segments, instead of of particular vehicle activities.

    A derivator can be added in the Observation block, that converts any raw data point by calculating the road segment
    corresponding to the vehicle position, and then generates an new observation mentioning the observed speed at this
    moment for the considered segment.

Because a first temporal aggregation has already been performed on observations corresponding to raw data points, such
an aggregation is not repeated here. Derived observations may benefit from a first aggregation, though. In that cas, this
first aggregation is performed in this block.



Aggregation Block
-----------------

.. figure:: figures/processor-flow-aggregation.png
   :align: center
   :width: 100 %
   :alt: Process Flow -- Aggregation Block

   Aggregation block of the Processor component.

The application of temporal derivators generates aggregated values that are collected by the Aggregation block.
The following operations are then applied on these values:

1. The aggregated values are converted into Kafka messages, that are sent to the ``aggregations`` topic. It enables
   other TSorage components and external tools to further process the values.

2. Dynamic tags associated with the aggregated values are reported to the Tag block for indexing purpose.

3. Aggregated values are stored in the time series database for later retrieval. An aggregated value can trigger the
   execution of follow-up, coarser time aggregators. The resulting aggregated values are sent back the entry of the
   Aggregation block, feeding a loop cycle that ends once all time aggregators have been processed.

Once stored, aggregated values are communicated to the Observation block, that can transform them into additional
observations.

.. _`Akka Stream`: https://doc.akka.io/docs/akka/2.5.5/scala/stream/stream-introduction.html

Storage
=======

Hub Services
============

Authentication
--------------

Tag Management
--------------