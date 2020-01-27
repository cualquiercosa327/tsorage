.. _CollectingTimeSeries:

**********************
Collecting Time Series
**********************


Collector Component
===================

The Collector component is part of the :ref:`collection layer <Archi_CollectionLayer>`.
It essentially is an application that runs on a device (server, virtual machine, embedded hardware, …) near a data source.
As its name suggests, the Collector component collects time series values either by querying the data source,
or by observing its environment.

The collector comes with **modules** that support the connection to various types of data source, including
relational databases, Modbus networks, OPC servers, etc. Each module creates one or many proactive agents, that interact
with their environment for actually collecting data points.
Using the collector component therefore consists in choosing and configuring the modules that must be run in order to
collect new observations.

Freshly collected data points are eventually submitted to a component of the :ref:`ingestion layer <Archi_IngestionLayer>`
for processing and storage purposes.
The second task of the user of the collector component is therefore to specify how the collected data stream must be
transmitted to the ingestion layer.

Both module and transmitted configuration are performed by editing a configuration file that is communicated to the
collector when its process is starting.

.. figure:: figures/collector-flows.png
   :align: center
   :width: 50 %
   :alt: Collector Flow

   Example of Collector flow, involving three data sources and a connector to the HTTP REST service for ingesting time series.

Configuration
-------------

The configuration file of a collector module contains two elements.

- The **sink** part describes how collected data points should be sent to the ingestion layer.
- The **sources** part describes the list of data sources from which data points must be collected. As mentioned above,
  each type of data source is covered by a specific collector component, and each component comes with its own configuration
  schema.

An example of configuration document is listed below:

.. code-block::

    sources = [
        { configuration for a RDMS connexion },
        { configuration for a Modbus connexion }
    ]

    sink = {
        type = "http",
        ip = "127.0.0.1",
        port = 8081,
        token = "ed1e6666-10f8-4cf5-bed9-610102457aa9"
    }



Data Point Buffering
********************

Sink Configuration
==================

In the Collector module, the sink is the component that submits extracted data points to the ingestion layer. Because this
layer relies on different technologies, the user has to specify how to connect the sink to an operating ingestion component.

For a REST HTTP ingestion service, the following parameters must be specified:

- ``type`` must be set to ``http``.
- ``ip`` is the IP address of the HTTP service.
- ``port`` is the port number the HTTP service is listening to. Default value: ``8080``.
- ``token`` is the token used for authenticating the sender.
  This token will be transmitted with each message submitted to the ingestion component.
  Any incoming message with no token, or a token that does not correspond to an authorized user, will be rejected by the
  ingestion layer. Authentication tokens are managed from the :ref:`User and Access Management <UserManagement>` interface.

For a MQTT ingestion service, the following parameters must be specified:

- ``type`` must be set to ``mqtt``.
- ``ip`` is the IP address of the MQTT broker.
- ``port`` is the port the MQTT broker is listening to. Default value: ``1883``.

.. warning::

    Since authentication tokens may be used for altering the state of a TSorage solution, they must be considered
    as secret items that should only be shared with trustworthy entities. Because such a token is stored in the collector
    configuration file, the access to this file should be restricted to only allowed users.


Modbus Module
=============

Introduction
------------

In the industry, `Modbus <http://www.modbus.org/specs.php>`_ is a *de facto* standard communication protocol
for connecting electronic devices.
It is broadly used for data acquisition and setting programmable logic controllers (PLCs).

In a Modbus network, a **master node** is responsible of initiating communication sessions,
while **slave nodes** can only react to requests from the master node, by changing their state or retrieving it as a
response to request.

The ModBus module of the Collector can only be used for collecting the current state of slave nodes.
For both RTU and TCP communications, the module operates in two different modes:

- **polling mode**, which consists in periodically sending frames to slaves nodes for querying their state.
  Obtained responses are submitted to an ingestion component.
- **sniffing**, a totally passive mode on which the module intercepts communication sessions and submits
  a copy of the communicated states to an ingestion component.

  The sniffing mode is particularly interesting for RTU communications, where only a single master is normally allowed on a network.
  It allows the Modbus module to seamlessly integrate with existing device networks.

Configuration
-------------

For a Modbus TCP polling, the following parameters must be specified:

- **type** must be `modbus_tcp_poll`
- **slaves** must be a list of Modbus slaves to be polled. For each of them, the following properties must be specified:

  - **name**, a human friendly name of the slave.
  - **ip**, the ip address (or the domain name) of the slave.
  - **port**, the port on which the slave is waiting for queries. The standard value is `502`.
  - **poll_frequency**, the desired number of milliseconds between two consecutive requests to the slave.
  - **output_coils**, the list of coils in the discrete output coils (coil numbers between 1 and 9999) to be collected.
  - **input_contacts**, the list of discrete input contacts (coils number between 10001 and 19999) to be collected.
  - **input_registers**, the list of analog input registers (registers number between 30001 and 39999) to be collected.
  - **holding_registers**, the list of analog output holding registers (registers number between 40001 and 49999) to be collected.

The coil and register tables (`output_coils`, `input_contacts`, `input_registers`, `holding_registers`) are polled
in a cyclic fashion, it that order.

After a poll cycle finishes, the next one is planned so that the interval between two consecutive poll starts
is approximately `poll_frequency` milliseconds.
The actual interval may vary according to network latencies, processing delays, and other hardly predictable perturbations.

If a polling cycle takes longer than the specified time interval, the cycle is started right after the prolonged one finishes.

Each item from in `output_coils` and `input_contacts` describes the properties of a coil as follows:

- ``address``, the coil address as an integer (ex: ``1234``) or an hexadecimal string (ex: ``"04D2"``). Must be a value
  between `0000` and `270E`.
- ``metric``, the id of the metric associated with the coil.
- ``tagset``, a optional dictionary of the dynamic tagset to be reported with each collected value.

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
            address = "04D3",
            metric = "parked-head",
            tagset = {
                asset_type = "drive head",
                computer = "14a284d5-4aa0"
            }
        }
    ]

Each item in `input_registers` and `holding_registers` describes as the properties of a register as follows:

- ``address``, the register address as an integer (ex: ``1234``) or an hexadecimal string (ex: ``"04D2"``). Should be a value
  between `0000` and `270E`.
- ``metric``, the id of the metric associated with the coil.
- ``tagset``, a optional dictionary of the dynamic tagset to be reported with each collected value.
- ``type``, the type of data stored in the register. It helps the module to properly decode the register, and transmit
  a message in the right format. Must be one of the following value:

  - ``bool16``, a boolean value stored in a single word.
  - ``uint16``, an unsigned integer value, stored as a single word.
  - ``uint32``, an unsigned integer value, stored as two words. The register that follows the addressed one will be used
    to read the value.
  - ``uint64``, an unsigned integer value, stored as four words. The tree registers that follow the addressed one will
    be used to read the value.
  - ``sint16``, a signed integer value, stored as a single word.
  - ``sint32``, a signed integer value, stored as two words. The register that follows the addressed one will be used
    to read the value.
  - ``uint64``, a signed integer value, stored as four words. The tree registers that follow the addressed one will
    be used to read the value.
  - ``ufloat16``, an unsigned floatting number, stored as a single word.
  - ``ufloat32``, an unsigned floatting number, stored as two words.
    The register that follows the addressed one will be used to read the value.
  - ``ufloat64``, an unsigned floatting number, stored as four words.
    The tree registers that follow the addressed one will be used to read the value.
  - ``sfloat16``, a signed floatting number, stored as a single word.
  - ``sfloat32``, a signed floatting number, stored as two words.
    The register that follows the addressed one will be used to read the value.
  - ``sfloat64``, a signed floatting number, stored as four words.
    The tree registers that follow the addressed one will be used to read the value.
  - ``enum16``, a categorial value, stored as a single word. The ``values`` property of the register can be used for
    decoding each possible integer as a string.
  - ``charN``, where `N` is a positive integer. Represent a character string stored as `N` words. Each word is decoded
    as a pair of characters, using the `ASCII encoding standard <https://en.wikipedia.org/wiki/ASCII>`_.
- ``ordering``, a string representing the way bytes and words are ordered in the register.
  Default is `HH`, and this property should have one of the following values:

  - ``HH``, for High byte first, High word first. Corresponds to the *big endian* representation.
  - ``HL``, for High byte first, Low word first.
  - ``LH``, for Low byte first, High word first.
  - ``LL``, for Low byte first, Low word first. Correspond to the *little endian* representation.

Numeric (ie, integer and floating) types can optionally have the following extra parameters:

- ``min_threshold``: a numeric value, below which collected values will automatically be associated with the tag
  ``quality=bad``.
- ``max_threshold``: a numeric value, above which collected values will automatically be associated with the tag
  ``quality=bad``.

Please note that, if the ``min_threshold`` or the ``max_threshold`` parameters are specified, and if the collected
value remains behind the thresholds, this value will automatically be associated with the tag ``quality=good``.

Additionally, The unsigned integer and enumeration types can optionally have the following extra parameters:

- ``mask``, the hexadecimal representation of a binary mask to be applied for limiting the value extraction to the bits
  covered by this mask. It allows, for instance, to represent (and decode) two 8-bit integers in a 16-bit register.

Furthermore, the ``enum16`` type can optionally have the following extra parameters:

- ``values``, a mapping between integers (represented as literal integers or hexadecimal strings) and arbitrary strings.

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

The snippet below shows an example of register description:

.. code-block::

    holding_registers = [
        {
            address = 1,
            metric = "pump-temperator",


        }
    ]




