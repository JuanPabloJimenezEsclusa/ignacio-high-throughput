# Kubernetes/Kind

> • [Dependencies](#-dependencies)
  • [Architecture](#-architecture)
  • [Usage](#-usage)
  • [Links](#-links)

Based on [Kind](https://kind.sigs.k8s.io/) (Kubernetes IN Docker)

## ⚙️ Dependencies

---

* [Docker ~28](https://docs.docker.com/engine/release-notes/28/)
* [Kind ~0.29](https://kind.sigs.k8s.io/docs/user/quick-start/#installation)

## 🏗️ Architecture

---

Basic deployment with Kind to prioritize simplicity and local development.

![High Throughput Kind](./images/high-throughput-k8s-diagram.svg)

## 🚀 Usage

---

### 📝 Hosts Configuration

Before starting the cluster, ensure your `/etc/hosts` file has the proper kind-registry entry

### 🔄 Cluster Management

```bash
cd clusters
./install.sh
./start-cluster.sh
./stop-cluster.sh
```

### 📡 API Deployment

```bash
# Deploy standard API
cd API
./apply.sh buildProjects=true
./delete.sh removeImages=true
```

### 🔄 Full Setup

> ⚠️ Requires sudo permissions to install tools and remove tmp files.

Use the combined script to set up everything at once:

```bash
# Run the full setup script
./run-cluster-with-api.sh buildProjects=true
```

## 🔗 Links

---

* **API:**
  * [imperative-throughput](http://localhost:8888/imperative-throughput/smokes)
  * [reactive-throughput](http://localhost:9999/reactive-throughput/smokes)
* **Prometheus (Monitoring):**
  * [Prometheus monitoring dashboard](http://localhost:30000)
* **Grafana (Monitoring Visualization):**
  * [Grafana dashboard](http://localhost:31000) (Login: admin/prom-operator)
