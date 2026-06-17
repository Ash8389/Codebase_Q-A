# Codebase Q&A

A **Retrieval-Augmented Generation (RAG)** system that lets you ask natural-language questions about any codebase. Built with Java Spring Boot microservices, it ingests source code, generates vector embeddings, stores them in Qdrant, and answers queries using semantic search — all wired together via Apache Kafka and cached with Redis.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Services](#services)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Infrastructure Setup](#infrastructure-setup)
- [Project Structure](#project-structure)
- [How It Works](#how-it-works)
- [API Reference](#api-reference)
- [Configuration](#configuration)
- [Contributing](#contributing)

---

## Architecture Overview

```
User Query
    │
    ▼
┌─────────────────────┐
│   API Gateway        │  ← Single entry point for all client requests
│   (port: varies)    │
└─────────┬───────────┘
          │
    ┌─────┴──────┐
    ▼            ▼
┌──────────┐  ┌──────────────┐
│  Ingest  │  │    Query     │
│  Service │  │   Service    │
└────┬─────┘  └──────┬───────┘
     │               │
     ▼               ▼
  Kafka           Redis (cache)
     │               │
     ▼               ▼
┌──────────────────────────┐
│    Embedding Service     │
│  (vector generation)     │
└──────────────┬───────────┘
               │
               ▼
        ┌─────────────┐
        │   Qdrant    │  ← Vector database (REST: 6333, gRPC: 6334)
        └─────────────┘
```

---

## Services

### `apiGatewayService`
The single entry point for all client-facing requests. Routes incoming HTTP requests to the appropriate downstream microservice (ingest or query). Handles cross-cutting concerns such as authentication, rate limiting, and request routing.

### `ingest_service`
Accepts a GitHub repository URL or source files, clones/reads the codebase, chunks the source files, and publishes chunked content to a Kafka topic for asynchronous downstream processing.

### `embedding_Service`
Consumes code chunks from Kafka, generates vector embeddings using a language model, and upserts the resulting vectors into the Qdrant vector database for persistent storage.

### `query-service`
Handles natural-language Q&A requests. Embeds the user's question, performs a vector similarity search against Qdrant to retrieve the most relevant code chunks, and synthesizes a contextual answer.

### `common-dto`
A shared library module containing common Data Transfer Objects (DTOs), request/response models, and shared utilities used across all microservices.

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java |
| Framework | Spring Boot |
| Message Broker | Apache Kafka 3.9.1 |
| Vector Database | Qdrant (latest) |
| Cache | Redis 7 (Alpine) |
| Containerization | Docker / Docker Compose |
| Build Tool | Maven / Gradle |

---

## Prerequisites

- **Docker** and **Docker Compose** installed
- **Java 17+** (or the version required by your Spring Boot version)
- **Maven** or **Gradle** for building the services

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Ash8389/Codebase_Q-A.git
cd Codebase_Q-A
```

### 2. Start the infrastructure

Spin up Qdrant, Redis, and Kafka using Docker Compose:

```bash
docker compose up -d
```

This starts:
- **Qdrant** on ports `6333` (REST) and `6334` (gRPC) with persistent storage
- **Redis** on port `6379` with a 256 MB LRU memory cap
- **Kafka** on port `9092` in KRaft mode (no Zookeeper required)

### 3. Build the services

Build all microservices from their respective directories:

```bash
# Example for ingest service
cd ingest_service/ingest_service
./mvnw clean install

# Repeat for each service:
# apiGatewayService/apiGatewayService
# embedding_Service/embedding_Service
# query-service/query-service
```

### 4. Run the services

Start each microservice in the correct order:

```bash
# 1. Start the embedding service first (Kafka consumer)
# 2. Start the ingest service
# 3. Start the query service
# 4. Start the API gateway
```

---

## Infrastructure Setup

The `docker-compose.yml` defines three infrastructure services:

### Qdrant (Vector Database)
```yaml
ports:
  - "6333:6333"   # REST API
  - "6334:6334"   # gRPC API
volumes:
  - qdrant_data:/qdrant/storage   # Persistent storage
```

### Redis (Cache)
```yaml
ports:
  - "6379:6379"
command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
```

### Kafka (Message Broker — KRaft mode)
```yaml
ports:
  - "9092:9092"
# Runs as both broker and controller (no Zookeeper needed)
```

To verify all containers are healthy:

```bash
docker compose ps
```

---

## Project Structure

```
Codebase_Q-A/
├── docker-compose.yml                          # Infrastructure services
├── common-dto/                                 # Shared DTOs and models
├── apiGatewayService/
│   └── apiGatewayService/                      # API Gateway Spring Boot app
├── ingest_service/
│   └── ingest_service/                         # Code ingestion service
├── embedding_Service/
│   └── embedding_Service/                      # Embedding generation service
└── query-service/
    └── query-service/                          # Q&A query service
```

---

## How It Works

### Ingestion Pipeline

1. **User submits** a repository URL or uploads source files via the API Gateway.
2. The **Ingest Service** clones the repository and splits source files into meaningful code chunks.
3. Chunks are published as messages to a **Kafka topic**.
4. The **Embedding Service** consumes chunks from Kafka, generates vector embeddings, and upserts them into **Qdrant**.

### Query Pipeline

1. **User submits** a natural-language question via the API Gateway.
2. The **Query Service** checks **Redis** for a cached response.
3. On cache miss, the question is embedded and used to perform a **vector similarity search** in Qdrant.
4. The top-k most relevant code chunks are retrieved and used to synthesize an answer.
5. The result is **cached in Redis** and returned to the user.

---

## API Reference

All requests go through the API Gateway. Typical endpoints:

### Ingest a Repository
```
POST /api/ingest
Content-Type: application/json

{
  "repoUrl": "https://github.com/username/repository"
}
```

### Ask a Question
```
POST /api/query
Content-Type: application/json

{
  "question": "How does the authentication flow work?"
}
```

> Exact endpoint paths depend on the gateway routing configuration in `apiGatewayService`.

---

## Configuration

Each service uses Spring Boot `application.properties` / `application.yml`. Key properties to configure:

| Property | Description | Default |
|---|---|---|
| `qdrant.host` | Qdrant server host | `localhost` |
| `qdrant.port` | Qdrant gRPC port | `6334` |
| `spring.kafka.bootstrap-servers` | Kafka broker address | `localhost:9092` |
| `spring.data.redis.host` | Redis host | `localhost` |
| `spring.data.redis.port` | Redis port | `6379` |

---

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'Add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

---

## License

This project is open source. See the repository for license details.
