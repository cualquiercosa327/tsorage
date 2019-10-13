# TSorage

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) 
![](https://github.com/cetic/tsorage/workflows/unit-tests/badge.svg)
[![Documentation Status](https://readthedocs.org/projects/tsorage/badge/?version=latest)](https://tsorage.readthedocs.io/en/latest/?badge=latest)

A platform for collecting, storing, and processing time series.

This project aims to provide a scalable, performant, generic, and open source plateform for collecting, 
storing, processing, and presenting time series.

A time series is defined as a collection of values, order by a timestamp associated with each value. 
In TSorage, a value could be anything as long as it can be represented as nested attribute-value pairs, i.e. a JSON object.
Its main purpose is to manage (I)IoT streams.

The project is made of different modules:

- Ingestion, for receiving value streams from external agents. HTTP, MQTT, and AMQP gateways are planned.
- Processor, for processing value streams. In particular, configurable rollups (temporal aggregations) are performed 
in order to provide faster responses to data queries. Arbitrary processing also includes 
  - Complex Event Processing, 
  - rule-based processing,
  - alerting,
  - process mining
- Hub, a set of microservices for exploiting the platform.