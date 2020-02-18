**********************
Installation Procedure
**********************


Installing Kafka
================

Installing Cassandra
====================


Installing a Collector Module
=============================


Time and Time Zone
------------------

The data points retrieved by a collector **must** be timestamped with the UTC timezone, so the conversion for display
to other time zones is easier and possible. For data sources that only expose the current value of the metric of interest,
it's the collector's responsibility to correctly timestamp any collected data point. In order to do so, this
component uses the clock time and time zone specified by the underlying host. These parameters must therefore be
set appropriately.

.. TODO: Mount /etc/localtime:/etc/localtime:ro and /etc/localtime:/etc/localtime:ro as volumes for the docker image of Collector

- Make sure the clock of the host is correctly set, and it will periodically be synced.
- Make sure the time zone of the host is correctly set.
- Appropriately set the :ref:`Collector configuration file <CollectingTimeSeriesConfiguration>`
- Fetch the Docker compose file for the Collector component
- Run the composition.
- Check in the ingestion layer that data points are actually ingested.


Installing an Ingestion Module
==============================


.. _INSTALL_MQTT_BROKER:

Installing a MQTT Broker
------------------------

The MQTT module of the ingestion layer relies on a MQTT broker for receiving incoming messages. That implies such a broker must be deployed on an software environment that can be reached from both the data collector agents and the ingestion module. We recommand `Mosquitto`_, a popular, open source MQTT broker that comes with prepared Docker images.

.. _`Mosquitto`: https://mosquitto.org/


Password File Preparation
^^^^^^^^^^^^^^^^^^^^^^^^^

When a text file is used for listing the users authorized to access a MQTT channel, this file must be prepared in order to store passwords hashed in a format similar to `crypt`_. First, a file containing the list of logins and their corresponding unencrypted password must be set:

.. code-block:: none

    steve:password
    jim:secret

Then, this file must be processed by `mosquitto_passwd` in order to hash the passwords:

.. code-block:: none

    mosquitto_passwd -U passwordfile

This command changes the password file, replacing the passwords by their hashes.


Configuration Settings
^^^^^^^^^^^^^^^^^^^^^^

Prepare a `configuration file`_ for describing how Mosquitto must behave. The snippet below shows a simple configuration file, that configures Mosquitto for accepting unencrypted communications from clients, and for using a password file for the authentication.

.. code-block:: none

    port 1883
    allow_anonymous false
    password_file /path/passwordfile
    persistence true
    persistence_location /mosquitto/data/
    log_dest file /mosquitto/log/mosquitto.log


Running Mosquitto
^^^^^^^^^^^^^^^^^

Finally, you can run Mosquitto and refer to the configuration file:

.. code-block:: none

    mosquitto -c mosquitto.conf

If you are using Docker, you can mount a local configuration file to ``/mosquitto/config/mosquitto.conf`` and use it from the running Docker image:

.. code-block:: none

    mkdir mosquitto
    mkdir mosquitto/config
    mkdir mosquitto/data
    mkdir mosquitto/config
    touch mosquitto/config/mosquitto.conf
    […]
    docker run -it -p 1883:1883 -p 9001:9001 -v mosquitto:/mosquitto/ eclipse-mosquitto

.. _`configuration file`: https://mosquitto.org/man/mosquitto-conf-5.html
.. _`crypt`: http://man7.org/linux/man-pages/man3/crypt.3.html


.. _INSTALL_KAFKA_TOPIC_CONFIG:



Installing Hub Services
=======================


.. _INSTALL_AUTH_MICROSERVICE:

Authentication Micro-service
----------------------------