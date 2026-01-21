# E-Commerce Search Service

A high-performance, event-driven search engine built with **Java (Vert.x)**, **Apache Kafka**, and **Apache Solr**. This service consumes product updates from Kafka and provides a rich REST API for full-text search, filtering, and faceted navigation.

## 🚀 Quick Start (From Scratch)

### Prerequisites

* Docker & Docker Compose
* Java 17+
* Maven 3.8+

### 1. Clean & Reset Environment

Ensure a fresh state by removing old containers and persistent Solr data:

```bash
docker compose down -v
rm -rf solr/product-core/data

```

### 2. Build the Service

Compile the Java application into a shadow (fat) JAR:

```bash
cd search-service
mvn clean package -DskipTests
cd ../docker

```

### 3. Launch Infrastructure

Start the middleware components and wait ~30s for Solr to initialize:

```bash
docker compose up -d zookeeper kafka solr

```

### 4. Start Search Service & Create Topic

Once infra is healthy, start the application and initialize the Kafka topic:

```bash
# Start Service
docker compose up -d search-service

# Create Topic
docker exec -it ecommerce-kafka kafka-topics \
  --bootstrap-server ecommerce-kafka:29092 \
  --create --topic product-search-events --partitions 1 --replication-factor 1

```

---

## 🏗 System Architecture

The system follows a reactive, event-driven pattern:

1. **Ingestion:** Products are pushed to the `product-search-events` Kafka topic.
2. **Indexing:** The `KafkaConsumerVerticle` consumes events and indexes them into **Apache Solr** in real-time.
3. **Serving:** The **Vert.x Web** API handles complex search queries using Solr’s DisMax parser.

---

## 📥 Data Ingestion (Example)

Stream data into the system using the Kafka console producer:

```bash
DO THIS INSIDE THE SEARCH_SERVICE

# Create a dummy products.jsonl file
echo '{"id":"p0001","name":"iPhone 15","description":"Apple smartphone","brand":"apple","category":"mobile","price":79999,"created_at":"2026-01-21T10:00:00Z"}' > products.jsonl

# Feed to Kafka
docker exec -i ecommerce-kafka kafka-console-producer \
  --bootstrap-server ecommerce-kafka:29092 \
  --topic product-search-events < products.jsonl

```

---

## 🔍 API Reference

**Base URL:** `http://localhost:8888`

### Search Products

`GET /api/search`

| Parameter | Type | Description |
| --- | --- | --- |
| `q` | String | Keyword search (Full-text) |
| `category` | String | Filter by category |
| `brand` | String | Filter by brand |
| `minPrice` | Number | Minimum price filter |
| `sort` | String | `price_asc`, `price_desc`, `newest` |

#### Example Requests:

* **Basic Search:** `curl "http://localhost:8888/api/search?q=apple"`
* **Complex Filter:** `curl "http://localhost:8888/api/search?q=phone&brand=samsung&minPrice=50000&sort=price_desc"`
* **Faceted Results:** All responses include a `facets` object for dynamic sidebar navigation.

---

## 🛠 Troubleshooting & Verification

* **Check Solr Index Count:**
  `curl "http://localhost:8983/solr/product/select?q=*:*&rows=0"`
* **View Application Logs:**
  `docker logs -f ecommerce-search-service`
* **Verify Kafka Consumer:**
  `docker exec -it ecommerce-kafka kafka-consumer-groups --bootstrap-server ecommerce-kafka:29092 --group search-service-group --describe`

---


---

# Example:

# 🔍 1. BASIC FULL-TEXT SEARCH

### Search by keyword

```bash
curl "http://localhost:8888/api/search?q=iphone"
```

### Search in description

```bash
curl "http://localhost:8888/api/search?q=smartphone"
```

### Partial keyword (tokenized)

```bash
curl "http://localhost:8888/api/search?q=galaxy"
```

---

# 🎯 2. RELEVANCE & BOOSTING TESTS

### Name boosted over description

```bash
curl "http://localhost:8888/api/search?q=apple"
```

👉 Products with **apple in name** should rank above description-only matches.

---

### Recency boost

```bash
curl "http://localhost:8888/api/search?q=mobile&sort=newest"
```

👉 Newer products appear first.

---

# 🧩 3. CATEGORY FILTERING

### Single category

```bash
curl "http://localhost:8888/api/search?q=apple&category=mobile"
```

### Different category

```bash
curl "http://localhost:8888/api/search?q=apple&category=wearable"
```

---

# 🏷️ 4. BRAND FILTERING

### Filter by brand

```bash
curl "http://localhost:8888/api/search?q=phone&brand=apple"
```

### Brand + category

```bash
curl "http://localhost:8888/api/search?q=phone&brand=samsung&category=mobile"
```

---

# 💰 5. PRICE RANGE FILTERING (VERY IMPORTANT)

### Min price only

```bash
curl "http://localhost:8888/api/search?q=phone&minPrice=30000"
```

### Max price only

```bash
curl "http://localhost:8888/api/search?q=phone&maxPrice=60000"
```

### Price range

```bash
curl "http://localhost:8888/api/search?q=phone&minPrice=30000&maxPrice=80000"
```

---

# 🔀 6. SORTING

### Price ascending

```bash
curl "http://localhost:8888/api/search?q=phone&sort=price_asc"
```

### Price descending

```bash
curl "http://localhost:8888/api/search?q=phone&sort=price_desc"
```

### Newest first

```bash
curl "http://localhost:8888/api/search?q=phone&sort=newest"
```

---

# 📄 7. PAGINATION

### First page (default)

```bash
curl "http://localhost:8888/api/search?q=phone&page=0&size=5"
```

### Second page

```bash
curl "http://localhost:8888/api/search?q=phone&page=1&size=5"
```

### Larger page size

```bash
curl "http://localhost:8888/api/search?q=phone&page=0&size=20"
```

---

# 🧠 8. COMBINED REAL-WORLD QUERIES (MOST IMPORTANT)

### ECommerce-style query

```bash
curl "http://localhost:8888/api/search?q=apple&category=mobile&brand=apple&minPrice=50000&sort=price_desc"
```

### Amazon-style browsing

```bash
curl "http://localhost:8888/api/search?q=phone&category=mobile&sort=price_asc"
```

### Wearable discovery

```bash
curl "http://localhost:8888/api/search?q=watch&category=wearable"
```

---

# 📊 9. FACET VALIDATION (VERY IMPORTANT)

Run:

```bash
curl "http://localhost:8888/api/search?q=phone"
```

Verify response contains:

```json
"facets": {
  "brand": {
    "apple": 10,
    "samsung": 6
  },
  "category": {
    "mobile": 14,
    "wearable": 2
  }
}
```

👉 Counts must match filtered result set.

---

# ❌ 10. NEGATIVE & EDGE CASES (PRODUCTION CHECK)

### No results

```bash
curl "http://localhost:8888/api/search?q=nonexistingproduct"
```

Expected:

```json
"items": []
```

---

### Invalid filter

```bash
curl "http://localhost:8888/api/search?q=phone&category=unknown"
```

Expected:

* No crash
* Empty result

---

### Extreme price

```bash
curl "http://localhost:8888/api/search?q=phone&minPrice=1000000"
```

---

# 🧪 11. REBUILD TEST (CRITICAL)

1️⃣ Stop search service
2️⃣ Delete Solr index
3️⃣ Replay Kafka events

Then run:

```bash
curl "http://localhost:8888/api/search?q=phone"
```

👉 Results should be back.

---

## 🧠 Key Features Validated

* [x] **Schema-on-Read:** Structured JSON ingestion.
* [x] **Faceted Search:** Dynamic counts for Brand and Category.
* [x] **Relevancy:** Boosting applied to product names over descriptions.
* [x] **Resilience:** Index can be rebuilt by replaying Kafka topics.
