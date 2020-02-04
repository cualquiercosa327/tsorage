# Kubernetes for TSorage

### Installation and run

Create `cetic-tsorage-dev` Kubernetes namespace:

```sh
kubectl create -f kube-namespaces.yaml
```

Install and run Cassandra:

```sh
helm install tsorage-cassandra incubator/cassandra --namespace cetic-tsorage-dev \
  --set config.cluster_size=1 # Set the number of Cassandra nodes.
```

Install and run Kafka:

```sh
helm install tsorage-kafka incubator/kafka --namespace cetic-tsorage-dev \
  --set replicas=1 # Set the number of Kafka brokers.
  --set zookeeper.replicaCount=1 # Set the number of ZooKeeper nodes.
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

### Access to Hub module.

```sh
curl http://<CLUSTER-IP>:<PORT>/api/v1/grafana
```

where `CLUSTER-IP` is the IP address of the Kubernetes cluster and `PORT` is `nodePort` of `tsorage-hub` service (in our configuration `nodePort` is set to `32081`). If you your Kubernetes cluster run on Minikube, you can get its IP address with the following command: `minikube ip`.

### Access to Grafana.

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

### Tricks

You can change the namespace in Kubernetes as following:

```sh
kubectl config set-context --current --namespace=cetic-tsorage-dev
```
