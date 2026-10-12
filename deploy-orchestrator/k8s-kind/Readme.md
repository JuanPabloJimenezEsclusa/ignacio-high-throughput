---
layout: default
title: orchestrator (k8s) (Kind)
---

# orchestrator (k8s) (Kind)

> • [Dependencies](#dependencies)
  • [Architecture](#architecture)
  • [Usage](#usage)
  • [Links](#links)

This deployment uses [Kind](https://kind.sigs.k8s.io/) (Kubernetes in Docker).

## Dependencies

* [Docker ~29](https://docs.docker.com/engine/release-notes/29/)
* [Kind ~0.33](https://kind.sigs.k8s.io/docs/user/quick-start/#installation)

## Architecture

A basic deployment that keeps the setup simple for local development.

![High Throughput Kind](./images/high-throughput-k8s-diagram.svg)

## Usage

### Hosts configuration

Before you start the cluster, make sure `/etc/hosts` holds the kind-registry entry.

### Cluster management

```bash
cd cluster
./install.sh
./start-cluster.sh
./stop-cluster.sh
```

### API deployment

```bash
cd API
# Deploy the standard API
./apply.sh buildProjects=true
./delete.sh removeImages=true
```

### Full setup

NOTE: This step needs `sudo` to install tools and to remove temporary files.

The combined script sets up everything at once:

```bash
./run-cluster-with-api.sh buildProjects=true
```

## Links

* Imperative module: http://localhost:8888/imperative-throughput/smokes
* Reactive module: http://localhost:9999/reactive-throughput/smokes
* Prometheus: http://localhost:30000
* Grafana: http://localhost:31000 (login `admin`; read the password with `kubectl get secret kind-prometheus-grafana -n monitoring-ns -o jsonpath="{.data.admin-password}" | base64 -d`)
