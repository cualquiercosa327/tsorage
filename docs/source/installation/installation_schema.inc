********************
Installation Schemas
********************

All-In-One
==========

.. warning::
    Because it provides no redundancy, the all-in-one schema offers neither highly available services,
    nor resiliency to software or hardware failure.

    This installation schema should only be used for demonstration or testing purpose.

The all-in-one schema consists in deploying a single instance of each module of interest in a single machine.
This is a good option for giving TSorage a try, but is definitively not appropriate for a production environment
since the failure of one component will result in the unavailability of the underlying services, and the failure
of the machine could cause a loss of the whole system.

For this schema, `Minikube`_ is recommended over the standard Kubernetes system for the container orchestration, since
it runs a single-node Kubernetes cluster inside a virtual machine and removes the burden of managing a distributed cluster.

 .. todo:: to be continued. Provide an HELM chart here for a default all-in-one deployment?

.. _`Minikube`: https://kubernetes.io/docs/setup/learning-environment/minikube/

Standard
========

Extended Installation
=====================

Cross-Site Installation
=======================

.. TODO Each site has at least one node for collecting and processing time series, plus N nodes dedicated to Cassandra. Each site has a local replication factor of 2, and a remote RF of 1, either towards the "mother site", or towards one of the other sites.

