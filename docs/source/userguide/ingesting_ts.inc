
.. _Ingesting_Time_Series:

*********************
Ingesting Time Series
*********************



.. _Ingesting_TS_HTTP_REST:

HTTP REST Ingestion Module
==========================

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

.. code-block::

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
=============




.. _Ingesting_TS_MQTT:

MQTT Ingestion Module
=====================

The MQTT ingestion module consumes messages from a MQTT broker. Consequently, :ref:`a MQTT broker must be installed <INSTALL_MQTT_BROKER>` before any use of this module.

The MQTT Ingestion module accepts TSorage messages represented as JSON objects (`mqtt/json` type) or `Protobuf`_ messages (`mqtt/pb` type).

The configuration of this module must contain the following parameters:

- ``kafka``, a :ref:`configuration of the Kafka topic <INSTALL_KAFKA_TOPIC_CONFIG>`.
- ``port``: The port on which the MQTT broker listen for incoming connections. Default is `1883`.
- ``max_connections``: The maximum number of simultaneous connections to the MQTT broker.
- ``buffer_size``: The maximum number of messages that can be bufferized before the ingestion process will back pressure the data flow (which can ultimately result in the forced closing of some incoming connections). Default is 1000.
- ``channel``: The name of the channel from which incoming messages will be consumed. Default is ``timeseries``.
- security: The security policy to implement. Se below for more details.

Example of configuration for the MQTT ingestion module:

.. code-block::

    {
        kafka = {
            host = localhost
            port = 9092
            topic = raw
        }

        security = {
            type = "password"
        }

        port = 1883
        max_connections = 50
        buffer_size = 1000
        channel = timeseries
    }

.. _`Protobuf`: https://developers.google.com/protocol-buffers

Anonymous Policy
----------------

The anonymous security policy allows the submission of data points without any protection. No authentication is required, and the data flow is transmitted in clear. This policy should only be used for testing and debug purposes.

The following parameters must be specified:

- ``type`` must be set to ``anonymous``.

Example of Anonymous policy configuration:

.. code-block::

    {
        type = "anonymous"
    }


Password Policy
---------------

This policy will enforces the security by submitting a login and a password during the initialisation of the session. Only authorized users are allowed to send data points. The data flow is transmitted in clear. This policy offers an interesting trade-off between the security and the ease of use.

The following parameters must be specified:

- ``type`` must be set to ``password``.
- ``login``, the user's login.
- ``password``, the password associated with the specified login.

Example of Password policy configuration (ingestion side):

.. code-block::

    {
        type = "password"
    }

Example of Password policy configuration (client side):

.. code-block::

    {
        type = "password"
        login = "my-login"
        password = "secret-password"
    }

SSL Policy
----------