**********************
Installation Procedure
**********************

Installing a Collector
======================


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