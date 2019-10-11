https://medium.com/@Ankitthakur/apache-kafka-installation-on-mac-using-homebrew-a367cdefd273

1. Install Java

``` 
brew cask install java
```

2. Install Kafka

```
brew install kafka
```

3. Run Zookeeper

```
zookeeper-server-start /usr/local/etc/kafka/zookeeper.properties
```

4. Run Kafka

```shell script
kafka-server-start /usr/local/etc/kafka/server.properties
```


To list kafka topics : 

```
kafka-topics --list --bootstrap-server localhost:9092
```

To create a new kafka topic : 

```
kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic raw
```


To consume Kafka messages on the console : 

```
kafka-console-consumer --bootstrap-server localhost:9092 --topic raw --new-consumer --from-beginning
```


To run Cassandra:

```
cd /apache-cassandra/bin
export JAVA_HOME=`/usr/libexec/java_home -v 1.8`; ./cassandra
```

To truncate a topic:

```
kafka-topics --bootstrap-server localhost:9092 --delete --topic raw
```