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

### Environment variables

TSorage uses several environment variables to configure certain parts of the software:

- `TSORAGE_CASSANDRA_HOST`: Host of Cassandra. Defaults to `localhost`.
- `TSORAGE_KAFKA_HOST`: Host of Kafka. Defaults to `localhost`.
- `TSORAGE_KAFKA_BROKER_ADDRESSES`: List of addresses of the Kafka brokers in a bootstrap Kafka cluster (more information [here](https://kafka.apache.org/documentation/) and [here](https://jaceklaskowski.gitbooks.io/apache-kafka/kafka-properties-bootstrap-servers.html)). Defaults to `[localhost:9092]`.
- `TSORAGE_HUB_LISTEN_ADDRESS`: Listen address of hub module. This variable controls which IP address to listen for incoming connections on. Defaults to `localhost`. If you use Docker, set this variable to `0.0.0.0`.
- `TSORAGE_HUB_HOST`: Host of hub module. Defaults to `localhost`.
- `TSORAGE_INGESTION_LISTEN_ADDRESS`: Listen address of ingestion module. This variable controls which IP address to listen for incoming connections on. Defaults to `localhost`. If you use Docker, set this variable to `0.0.0.0`.

### Deployment

In this project, we use [Docker Compose](https://docs.docker.com/compose/) for deployment. To run TSorage, make sure you have [Docker Engine](https://docs.docker.com/install/) and [Docker Compose](https://docs.docker.com/compose/install/) installed on your computer. Then, create Docker image of each module as follows:

```bash
sudo sbt hub/docker:publishLocal
sudo sbt ingestion/docker:publishLocal
sudo sbt processor/docker:publishLocal
```

Finally, run TSorage using this command in a terminal:

```bash
docker-compose up -d
```

To stop TSorage, run this command:

```bash
docker-compose down
```

To use Grafana, add a SimpleJson data source and put this HTTP URL:

```
http://hub:8081/api/v1/grafana
```

### Demonstration

[Datadog](https://www.datadoghq.com) agent is a service that sends metrics and events from your host to Datadog. In the case of our demonstration, we use [Docker image of the Datadog agent](https://hub.docker.com/r/datadog/agent) and have configured it to send data to the ingestion service.

To run the demonstration, use this command in a terminal:

```bash
docker-compose -f docker-compose.yml -f docker-compose.demo.yml up -d
```

To stop it, run this command:

```bash
docker-compose -f docker-compose.yml -f docker-compose.demo.yml down
```

### Current Default Ports

These are the default port configuration currently used by TSorage. This configuration is subject to change without prior warning.

- Cassandra : 9042
- HTTP ingestion: 8080
- HTTP services : 8081
- MQTT ingestion over TCP : 1883
- MQTT ingestion over TLS : 8883