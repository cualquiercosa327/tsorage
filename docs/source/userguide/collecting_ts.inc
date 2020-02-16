.. _CollectingTimeSeries:

**********************
Collecting Time Series
**********************

.. _CollectingTS_Collector_Component:

Collector Component
===================

The Collector component is part of the :ref:`collection layer <Archi_CollectionLayer>`. It essentially is an software product that can be deployed either as a containerized application on a server with access to its data sources, or in a small form factor device having the physical connectors required for performing the data collection. As its name suggests, the Collector component collects time series values either by querying the data source, observing its environment, or receiving informations from external data sources.

The collector comes with **modules** that support the connection *from* and *to* various types of data source, including relational databases, Modbus networks, OPC servers, etc. Each module creates one or many agents, that interact with their environment for actually collecting data points. Using the collector component therefore consists in choosing and configuring the modules that must be run in order to collect new observations.

Freshly collected data points are eventually submitted to a component of the :ref:`ingestion layer <Archi_IngestionLayer>` for processing and storage purposes. The second task of the user of the collector component is therefore to specify how the collected data stream must be transmitted to the ingestion layer.

Both collection and transmission configuration are performed by editing a configuration file that is communicated to the collector when its process is started.

.. figure:: figures/collector-flows.png
   :align: center
   :width: 50 %
   :alt: Collector Flow

   Example of Collector flow, involving three data sources and a connector to the HTTP REST service for ingesting time series.



component somehow extract the current value of a data source

.. _CollectingTimeSeriesConfiguration:

Configuration
-------------

The configuration file of a collector module contains three elements.

- The **buffer** part details how collected data points are temporarily stored before being sent to the ingestion layer.
  The configuration of the buffer should not be modified by the user.
- The **sources** part describes the list of data sources from which data points must be collected. As mentioned above,
  each type of data source is covered by a specific collector component, and each component comes with its own configuration
  schema.
- The **sinks** part describes the list outputs for the collected data points. On a typical configuration, this list contains a sinle sink that connects the collector to the ingestion layer. Each sink has its own configuration schema.

An example of configuration document is listed below:

.. code-block::

    buffer = {
        host = "localhost"
        port = 5672
        queue_name = "buffer"
        user = "guest"
        password = "guest"
    }

    sources = [
        { configuration for a RDMS connexion },
        { configuration for a Modbus connexion }
    ]

    sink = [
        { first sink configuration },
        { second sink configuration }
    ]



Source Configuration
====================

Each source is identified with the ``type`` attribute. Each type has its own configuration schema.
A source is either a *poll* source or a *drain* source.

Poll sources periodically act (typically by interacting with its environment or an external source) in order to fetch
new data points. These sources are characterized by the time interval separating two consecutive poll rounds.

While the collector tries to respect the specified time interval, the actual inter-round duration between typically varies
depending on unpredictable factors.
If a round takes more time to complete than the specified time interval, the next round will actually start right
after the current one comes to an end.

At any time, at most one round is delayed.
If the delay becomes so important that two rounds or more are awaiting to start, the oldest delayed rounds are cancelled.

On the other hand, drain sources are continuously listening for incoming data points pushed by external data sources, and
transmit new data points as soon as possible.


Poll Sources
------------

All poll sources have a mandatory ``interval`` attribute, which textually represents the expected time interval between
two successive poll rounds. The value of this attribute must be a :ref:`duration literal <UG_Duration_Literals>`.

Currently, the following source types are supported:

Random Poll Source
^^^^^^^^^^^^^^^^^^

This component randomly generates double values, mostly for testing purpose.
Each generated message contains a single data point associated with the current timestamp.

Mandatory parameters are:

- ``metric``, a list of texts representing the potential metric names to be associated with the message. Each time a
  message is generated, on of the specified metrics will be randomly picked.
- ``tagset``, a dictionary of `(key, value)` that will be added as the message tagset.
  Both keys and values must be textual.

Example of Random Poll configuration:

.. code-block::

    {
        type = "random"
        interval = "1 second"
        metric = [
            "sensor-1", "sensor-2", "sensor-3", "sensor-4", "sensor-5",
            "sensor-6", "sensor-7", "sensor-8", "sensor-9", "sensor-10"
        ]
        tagset = {
            "owner" = "myself",
            "quality" = "good"
        }
    }



.. _CollectingTimeSeries_Modbus_RTU:

Modbus RTU Poll Source
^^^^^^^^^^^^^^^^^^^^^^

The Modbus RTU Poll component frequently polls Modbus slaves over a RS232 network. Only once request is
submitted to the network at a time.

The configuration of this source supports the following parameters:

- ``type`` must be ``poll/modbus/rtu`` (should not be confused with :ref:`poll/modbus/rtu <CollectingTimeSeries_Modbus_RTU_Over_TCP>`).
- ``interval``, the desired polling interval.
- ``name``, a human friendly name of the network.
- ``max_response_bytes``, the maximum number of bytes a Modbus data response can contain. Requests will be sent in order to keep the responses length under this limit. Because Modbus response frames are limited to 256 bytes, the maximum value for this parameter is `252`.
- ``port``, the serial port on which the requests must be sent. Example: ``/dev/ttyS0``
- ``baud_rate`` [optional], an integer representing the baud rate. Depending on the master slaves, accepted values may include 300, 600, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200. Default is `9600`.
- ``parity`` [optional], the parity checking mode. Supported values are `no`, `odd`, and `even`. Please note the Modbus specification states the even parity is required. Default is `even`.
- ``output_coils``, the list of coils in the discrete output coils (coil numbers between 1 and 9999; Modbus function 1) to be collected.
- ``input_contacts``, the list of discrete input contacts (coils number between 10001 and 19999; Modbus function 2) to be collected.
- ``holding_registers``, the list of analog holding registers (registers number between 30001 and 39999; Modbus function 3) to be collected.
- ``input_registers``, the list of analog output input registers (registers number between 40001 and 49999; Modbus function 4) to be collected.
- ``response_timeout`` [optional], the period of time allowed to a slave for answering a request. Once this deadline has expired, the module no longer expect for an answer and termines the current message session. The timeout must be expressed as a :ref:`duration literal <UG_Duration_Literals>`. The default value is ``1 second``

The coil and register tables (`output_coils`, `input_contacts`, `input_registers`, `holding_registers`) are polled in a cyclic fashion, it that order.

The items in ``output_coils``, ``input_contacts``, ``holding_registers``, and ``input_registers`` are called :ref:`extracts <CollectingTimeSeries_Modbus_Extracts>` and describe the properties of Modbus registers.


.. _CollectingTimeSeries_Modbus_TCP:

Modbus TCP Poll Source
^^^^^^^^^^^^^^^^^^^^^^

The Modbus TCP Poll component frequently polls a Modbus TCP slave (server) for extracting values from its registers.

The configuration of this source supports the following parameters:

- ``type`` must be ``poll/modbus/tcp``
- ``interval``, the desired polling interval.
- ``name``, a human friendly name of the slave.
- ``max_response_bytes`` [optional], the maximum number of bytes a Modbus response can contain. Requests will be sent in order to keep the responses length under this limit. The total response length will be `9 + 2 * registerNumber`. The supported response length depends on the slave device.
- ``host``, the ip address or host name of the slave.
- ``port``, the port on which the slave is waiting for queries. The standard value is `502`.
- ``output_coils``, the list of coils in the discrete output coils (coil numbers between 1 and 9999; Modbus function 1) to be collected.
- ``input_contacts``, the list of discrete input contacts (coils number between 10001 and 19999; Modbus function 2) to be collected.
- ``holding_registers``, the list of analog holding registers (registers number between 30001 and 39999; Modbus function 3) to be collected.
- ``input_registers``, the list of analog output input registers (registers number between 40001 and 49999; Modbus function 4) to be collected.

The coil and register tables (`output_coils`, `input_contacts`, `input_registers`, `holding_registers`) are polled
in a cyclic fashion, it that order.

The snippet below shows an example of coil description:

.. code-block::

    output_coils = [
        {
            address = 1234,
            metric = "door-is-closed",
            tagset = {
                asset_type = "door"
            }
        },
        {
            address = 0x04D3,
            metric = "parked-head",
            tagset = {
                asset_type = "drive head",
                computer = "14a284d5-4aa0"
            }
        }
    ]
	
The items in ``output_coils``, ``input_contacts``, ``holding_registers``, and ``input_registers`` are called :ref:`extracts <CollectingTimeSeries_Modbus_Extracts>` and describe the properties of Modbus registers.

Because the Modbus TCP requests contain a transaction Id that is echoed by the responding slave, multiple requests can be submitted simultaneously (ie, without waiting the response to a request before sending the next request). Also, because the target slave is identified by its IP address, simultaneous communications can be established with multiple slaves. The Modbus TCP module adopts such behaviour, by simultaneously sending multiple requests for each of the declared source.


.. _CollectingTimeSeries_Modbus_RTU_Over_TCP:

Modbus RTU over TCP Poll Source
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Since Modbus RTU over TCP essentially consists in wrapping RTU messages in a TCP frame, this source is a mix between :ref:`Modbus RTU<CollectingTimeSeries_Modbus_RTU>` and :ref:`Modbus TCP<CollectingTimeSeries_Modbus_TCP>` sources.

The configuration of this source supports the following parameters:

- ``type`` must be ``poll/modbus/rtu-tcp``
- ``interval``, the desired polling interval.
- ``name``, a human friendly name of the slave.
- ``max_response_bytes`` [optional], the maximum number of bytes a Modbus response can contain. Requests will be sent in order to keep the responses length under this limit. The total response length will be `9 + 2 * registerNumber`. The supported response length depends on the slave device.
- ``host``, the ip address or host name of the slave.
- ``port``, the port on which the slave is waiting for queries. The standard value is `502`.
- ``output_coils``, the list of coils in the discrete output coils (coil numbers between 1 and 9999; Modbus function 1) to be collected.
- ``input_contacts``, the list of discrete input contacts (coils number between 10001 and 19999; Modbus function 2) to be collected.
- ``holding_registers``, the list of analog holding registers (registers number between 30001 and 39999; Modbus function 3) to be collected.
- ``input_registers``, the list of analog output input registers (registers number between 40001 and 49999; Modbus function 4) to be collected.
- ``response_timeout`` [optional], the period of time allowed to a slave for answering a request. Once this deadline has expired, the module no longer expect for an answer and termines the current message session. The timeout must be expressed as a :ref:`duration literal <UG_Duration_Literals>`. The default value is ``1 second``

The coil and register tables (`output_coils`, `input_contacts`, `input_registers`, `holding_registers`) are polled in a cyclic fashion, it that order.

The items in ``output_coils``, ``input_contacts``, ``holding_registers``, and ``input_registers`` are called :ref:`extracts <CollectingTimeSeries_Modbus_Extracts>` and describe the properties of Modbus registers.



.. _CollectingTimeSeries_Modbus_Extracts:

Modbus Extracts
^^^^^^^^^^^^^^^

For the Modbus RTU, Modbus RTU over TCP and Modbus TCP modules, the items in ``output_coils``, ``input_contacts``, ``holding_registers``, and ``input_registers`` are called `extracts` and describe the properties of Modbus registers as follows:

- ``unit_id``, the unit Id to which the Modbus requests will be addressed in order to perform the extract. For TCP connections, this parameter is generally not used by the slave and is set to ``0x00`` or ``0xFF`` by the master. 
- ``address``, the register address. Should be a value between `0000` and `0x270E`.
- ``metric``, the id of the metric associated with the coil.
- ``tagset``, a optional dictionary of the dynamic tagset to be reported with each collected value.
- ``type``, the type of data stored in the register. It helps the module to properly decode the register, and transmit
  a message in the right format. Must be one of the following value:

  - ``bool16``, a boolean value stored in a single word. When this type is used, the ``position`` attribute must be set with an integer between 0 (lowest bit) and 15 (highest bit), as the reference to the position of the word bit to be considered as a boolean.
  - ``ubyte``, an unsigned byte value. When this type is used, the ``byte`` attribute must be set to ``high`` or ``low``, as the reference to the position of the byte to be considered. 
  - ``sbyte``, an signed byte value. When this type is used, the ``byte`` attribute must be set to ``high`` or ``low``, as the reference to the position of the byte to be considered. 
  - ``uint16``, an unsigned integer value, stored as a single word.
  - ``uint32``, an unsigned integer value, stored as two words. The register that follows the addressed one will be used
    to read the value.
  - ``sint16``, a signed integer value, stored as a single word.
  - ``sint32``, a signed integer value, stored as two words. The register that follows the addressed one will be used
    to read the value.
  - ``sfloat32``, a signed floatting number, stored as two words.
    The register that follows the addressed one will be used to read the value.
  - ``enum16``, a categorial value, stored as a single word. The ``values`` property of the register can be used for
    decoding each possible integer as a string.
  - ``charN``, where `N` is a positive integer. Represent a character string stored as `N` words. Each word is decoded
    as a pair of characters, using the `ASCII encoding standard <https://en.wikipedia.org/wiki/ASCII>`_.
- ``byte_order``, a string representing the way bytes are ordered in the register. May be `LOW_BYTE_FIRST` or `HIGH_BYTE_FIRST`. Default is `HIGH_BYTE_FIRST`.
- ``word_order``, a string representing the way words are ordered in multi-register values. May be `LOW_WORD_FIRST` or `HIGH_WORD_FIRST`. Default is `HIGH_WORD_FIRST`.

Additionally, some data types accept extra parameters:

- Numeric types (ie, ``ubyte``, ``sbyte``, ``uint16``, ``uint32``, ``sint16``, ``sint32``, and ``sfloat32``) accept some threshold values for data sanity checking purpose. Please note that, if the ``min_threshold`` or the ``max_threshold`` parameters are specified, and if the collected value remains behind the thresholds, this value will automatically be associated with the tag ``quality=good``.
	
  - ``min_threshold``: a numeric value, below which collected values will automatically be associated with the tag ``quality=bad``.
  - ``max_threshold``: a numeric value, above which collected values will automatically be associated with the tag ``quality=bad``.
- The enumeration type (ie, ``enum16``) can optionally have the following extra parameters:
  
  - ``mask``, which can be applied for limiting the value extraction to the bits covered by this mask. The big endian binary representation of the specified value is used for converting this value to a mask. 
  - ``values``, a mapping between integers (represented as literal integers or hexadecimal strings) and arbitrary strings.
	
- Integer types (ie, ``ubyte``, ``sbyte``, ``uint16``, ``uint32``, ``sint16``, and ``sint32``) can optionnally have the ``rank`` parameter. This integer specifies the decimal rank of the integer unit. The integer value will be multiplied by `10^rank`. For instance, if a register supplies a temperature in tenths of a degree, the ``rank`` of the extract should be set to `1` in order to get the messages expressing the temperature in degrees. If the ``rank`` is specified (with a non-zero value), the type of the resulting messages will be ``tdouble`` instead of ``tlong``.


When the ``mask``, ``rank``, and ``values`` attributes are jointly used, they are applied on that order. For instance, the mask is applied on the raw register value, and then the resulting value is substituted by a text according to ``values``. 

.. topic:: Address and Mask Literals
	
	Addresses and masks values can be specified as an decimal string (e.g.: ``1234``, or ``"1234"``), an hexadecimal string (e.g.: ``0x04D2``, or ``"0x04D2"``), or an octal string (e.g.: ``01234``, or ``"01234"``).
	
	Because the (mandatory) heading zero of the octal representation may be confused with the (pointless) heading zero of the decimal representation, the octal representation should be avoided.

.. topic:: Register Extended Addresses

    According to the Modbus specifications, the register addresses must be comprised between `0000` and `270E`.
    Extended addresses, ie addresses from `270F` to `FFFF`, are supported by the Modbus module, but it is up to the user
    to ensure such addresses are supported by the queried slave nodes.

.. topic:: Byte and Word Ordering

    The Modbus specification doesn't mention how a register value should be decoded.
    Over time, device constructors proposed contradictory conventions about the bit ordering among a word,
    as well as the word ordering among records combining multiple words.

    Consequently, generic Modbus masters have to deal with any combination of high/low byte first, and high/low word
    first configurations.
	
    `HIGH_BYTE_FIRST` ordering is normally used in Modbus messages, and `HIGH_WORD_FIRST` is more frequently encountered. Using both `HIGH_BYTE_FIRST` and `HIGH_WORD_FIRST` corresponds to the `big endian` representation, while using both `LOW_BYTE_FIRST` and `LOW_WORD_FIRST` corresponds to the `little endian` representation.
	
	
The snippet below shows an example of extract description:

.. code-block::
	
    holding_registers = [
	{
		address = 0x1f
		metric = "pump-temperature"
		type = sint16
		rank = 2
		mask = 0x0F
		min_threshold = -100
		max_threshold = 100
    }]


Sink Configuration
==================

In the Collector module, the sink is the component that submits extracted data points to the ingestion layer.
Because this layer relies on different technologies, the user has to specify how to connect the sink to an
operating ingestion component.

.. warning::

    Since authentication tokens may be used for altering the state of a TSorage solution, they must be considered
    as secret items that should only be shared with trustworthy entities. Because such a token is stored in the collector
    configuration file, the access to this file should be restricted to only allowed users.


Stdout Sink
-----------

This simple sink prints every individual incoming message to the standard output of the running process.
A message is printed as a single line JSON document. Printed messages are separated by a new line character.

The following parameters must be specified:

- ``type`` must be set to ``stdout/json``


HTTP Sink
---------

The HTTP Sink can communicate with the :ref:`REST HTTP Ingestion module <Ingesting_TS_HTTP_REST>` of the ingestion layer.
Each message is sent using a HTTP POST query, the payload of which is a JSON representation of the message.

For a REST HTTP ingestion service, the following parameters must be specified:

- ``type`` must be set to ``http``.
- ``ip`` is the IP address of the HTTP service.
- ``port`` is the port number the HTTP service is listening to. Default value: ``8080``.
- ``prefix`` the path for the HTTP service.
- ``api_key`` is the token used for authenticating the sender.
  This token will be transmitted with each message submitted to the ingestion component.
  Any incoming message with no token, or a token that does not correspond to an authorized user, will be rejected by the
  ingestion layer. Authentication tokens are managed from the :ref:`User and Access Management <UserManagement>` interface.

Example of HTTP Sink configuration:

.. code-block::

    {
        type = "http"
        ip = "127.0.0.1"
        port = 8080
        prefix= "api/v1/series"
        api_key = "ed1e6666-10f8-4cf5-bed9-610102457aa9"
    }


MQTT Sink
---------

The MQTT Sink can communicate with the :ref:`MQTT ingestion module <Ingesting_TS_MQTT>` of the ingestion layer.
Messages are sent using a MQTT session. The following parameters must be specified:

- ``type`` must be set to ``mqtt``.
- ``ip`` is the IP address of the MQTT broker.
- ``port`` is the port the MQTT broker is listening to. Default value: ``1883``.
- ``channel`` must correspond to the channel used by the MQTT Ingestion module. Default value: ``timeseries``
- ``security``, the security policy to apply. It must be set accordingly to the :ref:`configuration of the ingestion module <INSTALL_MQTT_INGESTION>`.


