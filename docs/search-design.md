# E-commerce Product Search – System Design

## 1. Problem Statement
The goal of this system is to provide fast, relevant, and scalable product search
for an e-commerce platform. The system should support full-text search, filtering,
sorting, and pagination while remaining decoupled from the core transactional
database.

---

## 2. High-Level Architecture

MySQL is the source of truth for product data.
Search is treated as a derived, read-optimized system.

```
Data flow:

MySQL (Source of Truth)
        |
        ↓
Product Service (Vert.x)
        |
        | Kafka (Product Events)
        ↓ 
Search Ingestion Service (Vert.x)
        |
        ↓
    Solr Index
        |
        ↓
Search Query API (Vert.x)

```

---

## 3. Technology Stack

- Vert.x: Reactive, non-blocking backend services
- MySQL: Transactional data store (source of truth)
- Kafka: Event streaming and service decoupling
- Solr: Full-text search and filtering
- Docker: Local development and environment consistency

---

## 4. Data Ownership and Consistency

- MySQL owns product data correctness.
- Solr stores a denormalized and derived representation optimized for search.
- Kafka acts as the immutable event log.

Consistency model:
- Write path (MySQL): Strong consistency
- Read/Search path (Solr): Eventual consistency

---

## 5. Indexing Strategy

- Product changes are published as events to Kafka.
- Search Ingestion Service consumes events and updates Solr.
- Indexing is idempotent using product ID as the document key.
- Reprocessing events is safe and supported.

---

## 6. Search Capabilities

The search system supports:
- Full-text search on product name and description
- Filtering by category
- Price range filtering
- Sorting (price, recency)
- Pagination

Relevance strategy:
- Product name is boosted higher than description.
- Filters are applied using Solr filter queries.

---

## 7. Failure Handling and Recovery

- If Solr is unavailable, search requests fail gracefully.
- Product writes continue even if search indexing is unavailable.
- Kafka allows replaying events to rebuild the search index.
- Solr index is considered disposable and recoverable.

---

## 8. Non-Goals

The following are intentionally out of scope:
- User authentication and authorization
- Payments and checkout
- Inventory management
- Frontend implementation

---

## 9. Future Improvements

- Relevance tuning using analytics
- Autocomplete and suggestions
- Faceted search
- Redis caching for popular queries
- Dead-letter queue for failed events
