************
Introduction
************

About TSorage
=============

TSorage is a toolkit developed at the CETIC_ for collecting, storing, processing and presenting time series at scale. It is intended primarily for use by companies looking for a reliable, fast, cost-effective alternative to existing commercial solutions.

The emphasis of TSorage is on proposing services for managing time series of any type, with as few artificial constraints as possible. Among others, TSorage is designed with the following principles in mind.

**Limitation of vendor locking**. Technology evolves extremely quickly. This is especially true when it comes to the (I)IoT domain, where new ways manage and exploit sensors emerge every year. In order to mitigate the risk to make technological choices that turn out to be inappropriate in the future, TSorage is made of independant modules based on company baked, open source technologies. With such an approach, it's easier to update the platform when a new technology supersedes the existing one. All TSorage services are available through a REST API, which offers a standardized way to abstract the underlying technologies.

**Flexible type support**. Most sensors capture a continuous signal, such as a temperature or a pressure. However, time series also cover much more data types, such as geographic positions, trade transactions, and virtually any repetitive event. TSorage is designed to be easily extended in order to support your specific data types.

**Resilient and scalable processing**. Getting a high end infrastructure is a simple way to ensure the reliability of a service, up to the moment its needs start growing, leading to uncontrolled cost increasing. On the other hand, TSorage relies on commodity hardware to ensure a scalable and resilient service. By natively being a distributed solution, its capabilities are extended by simply adding more resources. When deployed in a multi-site fashion, TSorage offers local read and write performances while transparently supporting worldwide replication and synchronization.

**The data sources should scale as well**. Adding a new data source (such as a sensor) should be as simple and as fast as possible. Simply start feeding TSorage with a new data stream, and administrate it in a second time, either through a dedicated Web application or programmatically. Each value can be submitted with arbitrary properties (they are called *tags* in the TSorage terminology) that help querying and managing data sources more efficiently. Ultimately, the users no longer refer to a unique source id, but query, compare, and aggregate sources based on their tags.

**Fits in your infrastructure, ready for the Cloud**. When TSorage is used for managing sensible data, a deployment on premise may be preferred over using a remote hosting solution. For other use cases, a deployment on a public or private Cloud is a better option. In either case, TSorage is provided with deployment and monitoring scripts that reduce the burden of deploying and maintaining the solution.

.. _CETIC: https://www.cetic.be