# Kubernetes/Kind

> • [Dependencies](#-dependencies)
  • [Setup](#-setup)

Based on [Kind](https://kind.sigs.k8s.io/) (Kubernetes IN Docker)

## ⚙️ Dependencies

---

- [Kind >= 0.29.0](https://kind.sigs.k8s.io/docs/user/quick-start/#installation)

## 🚀 Setup

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
./apply.sh
./delete.sh
```

### 🔄 Full Setup

Use the combined script to set up everything at once:

```bash
# Run the full setup script
./run-cluster-with-api.sh
```
