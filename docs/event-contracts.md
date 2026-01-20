# Search Event Contracts

## Product Search Event
Kafka Topic: product-search-events

### Purpose
Carries the latest product state required for search indexing.

### Event Type
PRODUCT_UPSERT

### Versioning
- eventVersion is mandatory
- New fields must be backward compatible
- Breaking changes require a new version

### Replay Strategy
Search index can be rebuilt by replaying events from Kafka.

### Ownership
This contract is owned by the Search team.


### WHY UPSERT in product-search-event?
Search cares about the latest state and idempotent indexing.
UPSERT simplifies consumers and enables safe reindexing from Kafka.
