************
Architecture
************

Overview
========

The TSorage project is based on a modular architecture, with all modules being designed to be executed in distinct Docker containers. This makes TSorage a portable solution, with simple and standardized deployment steps. It also offers the possibility to place the components on different physical and virtual machines, making it available on a wide range of platforms and services.

Furthermore, (re)sizing a containerized architecture is easier, since a component can be moved to a platform offering more resources. Under certain conditions, containers can be duplicated in order to increase the performances of the underlying modules.

The figure below provides an overview of the TSorage architecture.

.. figure:: figures/architecture-overview.png
   :align: center
   :width: 100 %
   :alt: Architecture Overview

   Architecture Overview

Time Series processing starts with the :ref:`collection layer <Archi_CollectionLayer>`, that contains adhoc components for listening, collecting or extracting time series values from various data sources.

The :ref:`ingestion layer <Archi_IngestionLayer>` is the entry point for time series values. From this point, the described entities are considered as internal TSorage components, which basically means they are managed by the TSorage cluster. The ingestion layer is made of different *interface modules*, each of them providing a specific way for a data source to submit new time series values.



.. _Archi_CollectionLayer:

Collection Layer
================

The collection layer is made of remote components (a.k.a *collection agents*) that are not managed by a TSorage cluster. These components can be the result of third-party developments, and can can be deployed close to a data source for extracting data points from sensors, databases, spreadsheets, etc. An agent typically focuses on specific data sources, and is deployed near them for avoiding security or accessibility issues. Besides collecting data points, the purpose of an agent is to submit them to a component of the :ref:`ingestion layer <Archi_IngestionLayer>`.

At the core of the collection layer is the :ref:`Collector component <CollectingTS_Collector_Component>`, a systematic, modular and extensible tool that manages data extraction, its short-term storage, and its submission to the ingestion layer. It is designed to be deployed as a containerized application on a server with access to its data sources, as well as a small form factor device having the physical connectors required for performing the data collection.

`DataDog Agent`_, an open source tool developed by DataDog_ for collecting infrastructure- and application-related metrics, can also be used as a collection agent with only a simple adaptation of its configuration file.

.. _`DataDog Agent`: https://docs.datadoghq.com/agent/

.. _DataDog: https://www.datadoghq.com/


.. _Archi_IngestionLayer:

Ingestion Layer
===============

Components of the ingestion layer are responsible of accepting incoming connexions from collection agents for ingesting data points into TSorage. While basic checks (such as submitter authentication) are performed at this point, ingestion components are not supposed to perform data processing and try to forward the incoming data points as fast as possible. This ensures a highly effective ingestion, with important throughput rates. Ingestion components can easily be duplicated in order to face occasional or steady increasing needs for ingestion capacities.

Ingestion Components expect to receive :ref:`messages <INTRO_CONCEPTS_MESSAGE>`, represented either as a JSON value, or as Protobuf payload.

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

   Global view of the Processor Flow. Each named block corresponds to a processor graph.

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

The :ref:`dynamic tags <INTRO_CONCEPTS_TAGS>` mentioned in the messages are submitted to the *Tag block* for further processing.

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
    corresponding to the vehicle position, and then generates a new observation mentioning the observed speed at this
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

The storage of time series values and other TSorage-related informations is ensured by `Cassandra`_. Cassandra is a fault tolerant, performant, decentralized and scalable NoSQL database.

Cassandra stores raw data points and aggregated values in distinct tables having similar schemas. On these tables, each time series is sharded according to the timestamp, as describe in :ref:`Choose a Sharding <INSTALLATION_CHOOSE_SHARDING>`. On a typical installations, that means all the data points that relate to the same metric, the same dynamic tagset, and the same day or month (depending on the sharding preferencies) are stored as a single, time-ordered data block, so that requesting the data points corresponding to a time series for a continuous period of time is blazingly fast.

On typical setups, Cassandra will be deployed as a cluster having multiple nodes for ensuring high availability and failure resiliency. For :ref:`cross-site deployments <INSTALLATION_CROSS_SITE>`, the Cassandra nodes will be spread over geographically distinct sites, so that

1. Local data producers and consumers can use nearby Cassandra nodes, ensuring fast data submissions and retrievals.
2. Submissions are automatically propagated to the other cluster nodes, so that local submissions can be retrieved from an other site.
3. The database can survive the loss of an entire site.
4. If a site is temporarily disconnected from the rest of the cluster (for instance, because of a cutting off Internet access), each site can still operate normally. Cassandra nodes will automatically resynchronize once the connection is restored.

.. figure:: figures/overview-cross-site-cassandra.jpg
   :align: center
   :width: 70 %
   :alt: Overview of a cross-site Cassandra cluster

   Overview of a cross-site Cassandra Cluster. From `M. Ben Brahim`_.


.. _`Cassandra`: http://cassandra.apache.org/
.. _`M. Ben Brahim`: https://www.researchgate.net/publication/304624427_Spatial_data_extension_for_Cassandra_NoSQL_database

Hub Services
============

Authentication
--------------

Tag Management
--------------