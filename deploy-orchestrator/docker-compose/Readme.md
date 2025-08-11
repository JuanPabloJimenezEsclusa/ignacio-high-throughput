# Docker Compose

> • [Dependencies](#-dependencies)
  • [Architecture](#-architecture)
  • [Usage](#-usage)
  • [Links](#-links)

Based on `docker-compose`

## ⚙️ Dependencies

---

* [Docker ~28](https://docs.docker.com/engine/release-notes/28/)
* [Docker ~2.37](https://docs.docker.com/compose/install/standalone/)

## 🏗️ Architecture

---

Basic deployment with Docker Compose to prioritize simplicity and local development.

![High Throughput Docker](./images/high-throughput-docker-diagram.svg)

## 🚀 Usage

---

### Using Scripts

❗ **Note:** Config the `/etc/hosts` file to map the IP address `127.0.0.1` to the hostnames of the services defined in the `docker-compose` file.

```bash
# Start the services
./start.sh buildProjects=true
```
```bash
# Stop the services
./stop.sh removeImages=true
```

## 🔗 Links

---

* **API:**
  * [imperative-throughput](http://localhost:8888/imperative-throughput/smokes)
  * [reactive-throughput](http://localhost:9999/reactive-throughput/smokes)
* **Prometheus (Monitoring):**
  * [Prometheus monitoring dashboard](http://localhost:9090)
* **Grafana (Monitoring Visualization):**
  * [Grafana dashboard](http://localhost:3000) (Login: admin/admin)
* **Elasticsearch (Search Engine):**
  * [Elasticsearch access](http://localhost:9200/)
* **Kibana (Search Engine Visualization):**
  * [Dashboard](http://localhost:5601/app/kibana_overview)
  * [Indexes](http://localhost:5601/app/management/data/index_management/indices)
  * [Discover (Data View)](http://localhost:5601/app/discover#/)
