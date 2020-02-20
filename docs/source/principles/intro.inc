************
Introduction
************

About TSorage
=============

TSorage is a toolkit developed at the CETIC_ for collecting, storing, processing and presenting time series at scale.
It is intended primarily for use by companies looking for a reliable, fast, cost-effective alternative to existing commercial solutions.

The emphasis of TSorage is on proposing services for managing time series of any type, with as few artificial constraints as possible.
Among others, TSorage is designed with the following principles in mind.

**Availability and Scalability First**. Getting a high end infrastructure is a simple way to ensure the reliability of
a service, up to the moment its needs start growing, leading to uncontrolled cost increasing. On the other hand,
TSorage relies on commodity hardware to ensure a scalable and resilient service. By natively being a distributed solution,
its capabilities are extended by simply adding more resources. When deployed in a multi-site fashion, TSorage offers
local read and write performances while transparently supporting worldwide replication and synchronization.
When a site recovers from a connection failure, it should automatically resynchronize with the other sites of the group.

**Limitation of vendor locking**. Technology evolves extremely quickly.
This is especially true when it comes to the (I)IoT domain, where new ways manage and exploit sensors emerge every year.
In order to mitigate the risk to make technological choices that turn out to be inappropriate in the future,
TSorage is made of independant modules based on company baked, open source technologies.
With such an approach, it's easier to update the platform when a new technology supersedes the existing one.
All TSorage services are available through a REST API, which offers a standardized way to abstract the underlying technologies.

**Flexible type support**. Most sensors capture a continuous signal, such as a temperature or a pressure.
However, time series also cover much more data types, such as geographic positions, trade transactions,
and virtually any repetitive event. TSorage is designed to be easily extended in order to support your specific data types.

**The data sources must scale as well**. Adding a new data source (such as a sensor) should be as simple and as fast as possible.
Simply start feeding TSorage with a new data stream, and administrate it in a second time, either through a
dedicated Web application or programmatically. Each value can be submitted with arbitrary properties
(they are called *tags* in the TSorage terminology) that help querying and managing data sources more efficiently.
Ultimately, the users no longer refer to a unique source id, but query, compare, and aggregate sources based on their tags.

**Fits in your infrastructure, ready for the Cloud**. When TSorage is used for managing sensible data,
a deployment on premise may be preferred over using a remote hosting solution.
For other use cases, a deployment on a public or private Cloud is a better option.
In either case, TSorage is provided with deployment and monitoring scripts that reduce the burden of
deploying and maintaining the solution.

.. _CETIC: https://www.cetic.be

Main Concepts
=============

The most fundamental principle of TSorage is the concept of **metric**. A metric is an abstract entity that is attached with chronologically ordered measurements. Each measurement is called a **data point**, or an **observation**.

In TSorage, all the data points belonging to a metric represent the same physical, digital or logical phenomenon, and therefore typically have the same **data type** (although this is not a technical limitation of the platform). The data type of an observation determines how TSorage stores and presents it, as well as the transformations this observation can be subject to.

.. _INTRO_CONCEPTS_TAGS:

**Tags** are properties associated with data points. Their purpose is to help the user understanding the meaning of a particular data points, or querying data points having a particular meaning. Concretely, a tag is an arbitrary textual text (the **key**), associated with an arbitrary textual value (the **value**). Unsurprisingly, the set of tags associated with a data point is called its **tagset**. On distinguishes the **dynamic tags** that are directly attached to a data point, from the **static tags** that are attached to a metric and are automatically inherited to all the data points belonging to this metric.

In TSorage, a metric associated with a tagset is called a **time series**. There are two typical ways to use the tagsets. According to the first approach, a metric identifies a sensor (in the broad sens of the term), while tags clarify the status of the sensor (name of the manufacturer, geographical position, etc.) or some of the generated data points (data quality, operational condition, etc.)

With respect to the second approach, the metric refers to a property of interest (CPU usage, for instance), while the tags refer to the item at the origin of the data points (server number 5, for instance).

While mixing the approaches is technically possible, we recommend to choice one of them and stick with it.

Similarly to `the Spotify's Heroic documentation <https://spotify.github.io/heroic/docs/data_model#series>`__, we recommend the use of tagsets for enforcing a semantic series policy. Instead of placing information in the metric's name, using some enterprise specific nomenclature that is tryingly respected and therefore suffers many exceptions, the metric's name should be almost meaningless, and the semantic should be specified as a set of tags.

Please note that such a policy does not prevent the user from organizing the metrics in a hierarchic way, since tag keys can be used for defining the hierarchical levels of organization. See :ref:`TagManagement`.

The combination of a metric with a tagset is called a **time series**. Time series are organized hierarchically, since data points and aggregated values from different time series having the same metric can be combined in order to produce a more generic time series.

For instance, consider we have three time series for the metric ``my-temperature``, having the following tagsets:

- ``{"unit": "celcius"}``
- ``{"unit": "celcius", "quality": "good"}``
- ``{"unit": "celcius", "quality": "bad"}``

When a :ref:`data query<Feature_DataQuerying>` is submitted to the system, asking for values belonging to ``my-temperature`` with an empty tagset, these three time series will be extracted and transparently merged in order to present all the relevant values. In other words, both values with a good quality, a bad quality, or no quality specified will be taken into account.

Despite the fact the technologies involved in TSorage are quite efficient and scale pretty well, be aware extracting and merging multiple time series may be resource consuming. Therefore, the number of time series that must be merged in order to satisfy a data query should remain *reasonable* in order to keep the pressure on the data provider at an acceptable level.


.. _INTRO_CONCEPTS_MESSAGE:

Message
-------

Exchanging one data point at a time would be highly inefficient, in particular when a request-response protocol is used for communicating. In order to provide better performances, TSorage communications are based on the concept of message. A message is essentially a set of data points that relate to the same time series. In other words, messages are a way to submit submitting multiple data points at once, while only submitting the metric name, the dynamic tagset, and the data type once per message.

Each message contains to the following elements:

- ``metric``, the metric id for which new data points are provided.
- ``tagset``, a object representing the dynamic tagset associated with all the data points described in the message.
- ``type``, the type of all the data points described in the message. While using the same type of all data point relating to a metric is generally considered as a good practice, the type associated with a metric can change from a message to another.
- ``values``, a list of data points. Each data point is likewise a list of two elements. The first element is a representation of the timestamp associated with the data point, while the second element is the data point value.

TSorage components the user may have to interact with can represent a message as a JSON_ value, as well as a Protobuf_ message (using the proto3 language). The representation format actually used by the components depends on the configuration of the concerned components.

JSON Format
^^^^^^^^^^^

The JSON representation of a TSorage message must comply with the following `JSON schema`_:

.. code-block:: json

    {
      "$id": "be.cetic.tsorage.messageschema.json",
      "type": "object",
      "properties": {
        "metric": {
          "type": "string"
        },
        "tagset": {
          "type": "object",
          "additionalProperties": {
            "type": "string"
          }
        },
        "type": {
          "type": "string"
        },
        "values": {
          "type": "array",
          "items": [
            {
              "type": "array",
              "items": [
                {
                  "type": "string",
              "pattern": "^(-?(?:[1-9][0-9]*)?[0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])(\\.[0-9]+)?(\\.([0-9]){1,3})?$"
                },
                {}
              ]
            }
          ]
        }
      },
      "required": [
        "metric",
        "type",
        "values"
      ]
    }

As described in this schema, the tagset attribute must be a dictionnary of strings. Each value (that represents a data point) is a tuple (represented by an array in JSON) containing the timestamp and the value of the data point, in that order. The timestamp is represented by a string with the `ISO 8601`_ format.

The value itself may be any valid JSON object. Its actual schema depends on the specified data type. There are several builtin data types proposed by TSorage, and :ref:`arbitrarily complex extra data types can be added <INSTALL_EXTRA_DATA_TYPES>` at will.

The snippet below is an example of valid message in JSON format.

.. code-block:: json

    {
      "metric": "my-temperature-sensor",
      "tagset": {
       	"quality": "good",
        "owner": "myself"
      },
      "type": "tdouble",
      "values": [
        [ "2020-01-02T03:04:05.678", 42.1337 ],
        [ "2020-01-02T03:04:06.123", 654.72 ]

      ]
    }

Protobuf Format
^^^^^^^^^^^^^^^

In order to further improve message exchanges, a binary encoding alternative to JSON is proposed, in the form of a Protocol Buffer (a.k.a Protobuf_). Messages encoded that way must comply with the following `Protobuf structure definition`_ :

.. code-block:: protobuf

    syntax = "proto3";

    package  com.google.protobuf.util;
    option java_package = "com.google.protobuf.util";

    import "google/protobuf/timestamp.proto";

    message MessagePB {

        message Value
        {
             .google.protobuf.Timestamp datetime = 1;
             bytes payload = 2;
        }

        string metric = 1;
        map<string,string> tagset = 2;
        string type = 3;
        repeated Value values = 4;
    }

The payload is encoded as a byte array, the structure of which depends on the specified data type.

 **In the general case**, the payload (which is internally represented as a JSON value) is converted into its (compact) textual representation. The `UTF8 encoding`_ of this text is then used as the payload attribute. The use of UTF8 allows the simple representation of textual contents with non-latin characters. However, this encoding is generally quite inefficient, since each digit of a numeric value is represented by (at least) one byte, the name of each part of the encoded value is encoded as an UTF8 string, and some additional bytes are used for representing the structural organization of the payload.

 In order to mitigate this issue, values with the ``tlong`` and ``tdouble`` data types are directly encoded as 8-byte signed long and double values (respectively), using the `big endian`_ convention. In IoT-like contexts, these data types are likely to be the most commonly used, so that the Protobuf encoding turns out to be a highly efficient message representation.

.. _JSON : https://www.json.org/json-en.html
.. _Protobuf: https://developers.google.com/protocol-buffers/
.. _`JSON schema`: https://json-schema.org/understanding-json-schema/
.. _`ISO 8601`: https://en.wikipedia.org/wiki/ISO_8601
.. _`Protobuf structure definition`: https://github.com/cetic/tsorage/blob/dev/common/src/main/protobuf/message.proto
.. _`UTF8 encoding`: https://tools.ietf.org/html/rfc3629
.. _`big endian`: https://en.wikipedia.org/wiki/Endianness


.. _SECURITY_POLICIES:

Security Policies
-----------------

Anonymous Policy
^^^^^^^^^^^^^^^^

The anonymous security policy allows data point sharing without any protection. No authentication is required, and the data flow is transmitted unencrypted. This policy should only be used for testing and debug purposes.

The following parameters must be specified:

- ``type`` must be set to ``anonymous``.

Example of Anonymous policy configuration:

.. code-block:: none

    {
        type = "anonymous"
    }

Password Policy
^^^^^^^^^^^^^^^

This policy will enforces the security by submitting a login and a password during the initialisation of the session. Only authorized users are allowed to share data points. Both the data flow and the account parameters are transmitted unencrypted. This policy offers an interesting trade-off between the security and the ease of use.

The following parameters must be specified:

- ``type`` must be set to ``password``.
- ``login``, the user's login.
- ``password``, the password associated with the specified login.

Example of Password policy configuration:

.. code-block:: none

    {
        type = "password"
        login = "my-login"
        password = "secret-password"
    }


SSL Policy
^^^^^^^^^^

This policy is currently not supported.




