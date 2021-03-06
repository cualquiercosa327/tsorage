
.. _Ingesting_Time_Series:

*********************
Ingesting Time Series
*********************

The ingestion component, which is responsible for receiving data points from the outside (typically, from a :ref:`collector agent <CollectingTS_Collector_Component>`), is essentially defined as a list of :ref:`source modules <INGESTION_SOURCE_MODULES>`, that are waiting for new data points, and a list of :ref:`sink modules <INGESTION_SINK_MODULES>`, that are responsible for forwarding these data points to the TSorage internal components.

While multiple ingestion components can be deployed in order to face an increasing data volume, a typical installation initially contains a single ingestion module with as many source and sink modules as necessary. Source modules can be added to its configuration for supporting different kinds of data ingestion, while a single :ref:`Kafka sink <INGESTION_KAFKA_SINK>` is used, in most cases.


.. _INGESTION_SOURCE_MODULES:

Source Modules
==============

.. _Ingesting_TS_HTTP_REST:

HTTP REST Ingestion Module
--------------------------

The HTTP Ingestion module provides an HTTP server that listens to incoming HTTP requests for ingesting new messages. Because any HTTP client library, or even the `curl` tool, can be used for submitting data points, this module offers one of the most convenient entry point to TSorage. On the other hand, each submission requires an HTTP handshake, so this method comes with an overhead that can become substantial when the module is under heavy load.

This module only allows authenticated clients to submit data points. The authentication is ensured by a :ref:`Authentication micro-service <INSTALL_AUTH_MICROSERVICE>`, the use of which must be configured in the configuration file of this module.

The configuration of this module must contain the following parameters:

- ``kafka``, a :ref:`configuration of the Kafka topic <INSTALL_KAFKA_TOPIC_CONFIG>`.
- ``port``: The port from which the HTTP server is listening for incoming connexion. Default is `8080`.
- ``authentication``, the configuration for accessing the authentication micro-service. It must contain the following parameters:
  - ``host``, the host name or ip address on which the authentication micro-service is running.
  - ``port``, the port on which the authentication micro-service is listening for incoming connections. Default is ``8081``.
  - ``path``, the URL path of the service. Default is ``/api/v1/auth``.

Example of configuration for the HTTP ingestion module:

.. code-block:: none

    {
        kafka = {
            host = localhost
            port = 9092
            topic = raw
        }

        authentication = {
            host = "localhost"
            port = 8081
            path = "/api/v1/auth"
        }

        port = 8080
    }


HTTP over TLS
^^^^^^^^^^^^^





.. _Ingesting_TS_MQTT:

MQTT Ingestion Module
---------------------

The MQTT ingestion module consumes messages from a MQTT broker. Consequently, :ref:`a MQTT broker must be installed <INSTALL_MQTT_BROKER>` before any use of this module.

The MQTT Ingestion module accepts TSorage messages represented as JSON objects (`mqtt/json` type) or `Protobuf`_ messages (`mqtt/pb` type).

The configuration of this module must contain the following parameters:

- ``kafka``, a :ref:`configuration of the Kafka topic <INSTALL_KAFKA_TOPIC_CONFIG>`.
- ``port``: The port on which the MQTT broker listen for incoming connections. Default is `1883`.
- ``max_connections``: The maximum number of simultaneous connections to the MQTT broker.
- ``buffer_size``: The maximum number of messages that can be bufferized before the ingestion process will back pressure the data flow (which can ultimately result in the forced closing of some incoming connections). Default is 1000.
- ``channel``: The name of the channel from which incoming messages will be consumed. Default is ``timeseries``.
- security: The security policy to implement. Must correspond to the :ref:`installation choices <INSTALL_MQTT_BROKER>`. See below for details.

Example of configuration for the MQTT ingestion module:

.. code-block:: none

    {
        port = 1883
        max_connections = 50
        buffer_size = 1000
        channel = timeseries

        security = {
            type = "password"
            login = "steve"
            password = "password"
        }
    }

.. include:: anonymous_policy.inc
.. include:: password_policy.inc
.. include:: ssl_policy.inc


.. _INGESTION_SINK_MODULES:

Sink Modules
============

Stdout Sink
-----------

When this sink is mentioned in the configuration of an ingestion component, a one-line JSON representation of every ingested message is written to the standard output of the underlying system. This module is principally used for testing and debugging purposes, and does not actually forward the incoming messages to internal TSorage components. It should therefore be used with a :ref:`Kafka sink <INGESTION_KAFKA_SINK>`.


.. _INGESTION_KAFKA_SINK:

Kafka Sink
----------

All internal TSorage components communicate each others through `Kafka topics`_. In particular, whatever the way data points are ingested by an ingestion component, they are published in the ``raw`` Kafka topic for further processing. Consequently, a typical ingestion component contains one Kafka sink through which the incoming data flow is actually processed.

The configuration of this sink must have the following properties:

- ``type``, must be ``kafka``.
- ``topic``, the name of the Kafka topic on which incoming data points will be published. Default is ``raw``.
- ``nodes``, a list of Kafka nodes that can be used for publishing data points. The configuration of each node must contain the following properties:

   - ``host``, the IP address or the domain name of the name.
   - ``port``, the port on which the node is listening for incoming connections. Default is ``9092``.

Example of configuration for a Kafka sink:

.. code-block:: none

    {
        type = "kafka"
        topic = "raw"
        nodes = [
            {
                host = "localhost"
                port = 9092
            }
        ]
    }


.. _`Kafka topics`: https://kafka.apache.org/documentation/