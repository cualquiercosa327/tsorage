# Kubernetes for TSorage

### Installation and run

Create `cetic-tsorage-dev` Kubernetes namespace:

```sh
kubectl create -f kube-namespaces.yaml
```

Install and run Cassandra:

```sh
helm install tsorage-cassandra incubator/cassandra --namespace cetic-tsorage-dev \
  --set config.cluster_size=1 \
  --set resources.requests.memory=4Gi \
  --set resources.limits.memory=8Gi \
  --set resources.requests.cpu=2 \
  --set resources.limits.cpu=6 \
  --set config.max_heap_size=4096M \
  --set config.heap_new_size=1024M
# 'config.cluster_size' sets the number of Cassandra nodes.
```

Install and run Kafka:

```sh
helm install tsorage-kafka incubator/kafka --namespace cetic-tsorage-dev \
  --set replicas=1 \
  --set zookeeper.replicaCount=1
# 'replicas' sets the number of Kafka brokers.
# 'zookeeper.replicaCount' sets the number of ZooKeeper nodes.
```

Install and run Grafana (without any preconfigured data source):

```sh
helm install tsorage-grafana stable/grafana --namespace cetic-tsorage-dev \
  --set persistence.enabled=true \
  --set service.type=NodePort,service.nodePort=32082 \
  --set plugins=grafana-simple-json-datasource
```

Install and run Grafana (with a preconfigured data source):

```sh
helm install tsorage-grafana stable/grafana --namespace cetic-tsorage-dev -f kube-grafana-values-dev.yaml
```

Run Hub module:

```sh
kubectl apply -f kube-hub-conf-dev.yaml -f kube-hub-dev.yaml
```

Run Ingestion module:

```sh
kubectl apply -f kube-ingestion-conf-dev.yaml -f kube-ingestion-dev.yaml
```

Run Processor module:

```sh
kubectl apply -f kube-processor-conf-dev.yaml -f kube-processor-dev.yaml
```

Install and run Datadog (for a demonstration):

```sh
helm install tsorage-datadog stable/datadog --namespace cetic-tsorage-dev \
  --set datadog.apiKey=6bd66ef491424668aa0175e6ad1b2a99 \
  --set datadog.site=datadoghq.eu \
  --set datadog.dd_url=http://tsorage-ingestion.cetic-tsorage-dev.svc.cluster.local:8080
```

Check pods:

```sh
kubectl get pods --namespace=cetic-tsorage-dev
```

To access to the Cassandra container: 

```sh
kubectl exec --namespace=cetic-tsorage-dev -it tsorage-cassandra-0 cqlsh
```

Run a Kubernetes proxy server in another terminal window:

```sh
kubectl proxy
```

### Access to services.

#### Hub module.

```sh
curl http://<CLUSTER-IP>:<PORT>/api/v1/grafana
```

where `CLUSTER-IP` is the IP address of the Kubernetes cluster and `PORT` is `nodePort` of `tsorage-hub` service (in our configuration `nodePort` is set to `32081`). If you your Kubernetes cluster run on Minikube, you can get its IP address with the following command: `minikube ip`.

#### Ingestion module.

```sh
curl http://<CLUSTER-IP>:<PORT>/
```

where `CLUSTER-IP` is the IP address of the Kubernetes cluster and `PORT` is `nodePort` of `tsorage-hub` service (in our configuration `nodePort` is set to `32080`). If you your Kubernetes cluster run on Minikube, you can get its IP address with the following command: `minikube ip`.

#### Grafana.

Access to this URL in your web browser: `http://<CLUSTER-IP>:<PORT>/`, where `CLUSTER-IP` is the IP address of the Kubernetes cluster and `PORT` is `nodePort` of `tsorage-grafana` service (in our configuration `nodePort` is set to `32082`). If you your Kubernetes cluster run on Minikube, you can get its IP address with the following command: `minikube ip`.

The username name is `admin` and the passward can be obtained with this command:

```sh
kubectl get secret --namespace cetic-tsorage-dev tsorage-grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```

Add a SimpleJson data source and put this HTTP URL:

```
http://tsorage-hub.cetic-tsorage-dev.svc.cluster.local:8081/api/v1/grafana
```

### Uninstallation

Uninstall Cassandra:

```sh
helm uninstall tsorage-cassandra --namespace=cetic-tsorage-dev
```

Uninstall Kafka:

```sh
helm uninstall tsorage-kafka --namespace=cetic-tsorage-dev
```

Uninstall Grafana:

```sh
helm uninstall tsorage-grafana --namespace=cetic-tsorage-dev
```

Uninstall Hub module:

```sh
kubectl delete -f kube-hub-conf-dev.yaml -f kube-hub-dev.yaml
```

Uninstall Ingestion module:

```sh
kubectl delete -f kube-ingestion-conf-dev.yaml -f kube-ingestion-dev.yaml
```

Uninstall Processor module:

```sh
kubectl delete -f kube-processor-conf-dev.yaml -f kube-processor-dev.yaml
```

Uninstall Datadog:

```sh
helm uninstall tsorage-datadog --namespace=cetic-tsorage-dev
```

### Tricks

You can change the namespace in Kubernetes as following:

```sh
kubectl config set-context --current --namespace=cetic-tsorage-dev
```
