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

Time Series
-----------

Data Point
----------

Tag
---

.. _INTRO_CONCEPTS_MESSAGE:

Message
-------

.. todo:: json and protobuf representations


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




