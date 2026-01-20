🔹 Why text_all?
Allows simple querying while keeping individual fields for boosting.

🔹 Why docValues on numeric/date fields?
Required for efficient sorting and range filtering.

🔹 Why no joins or nested docs?
Simpler indexing, faster queries, easier reindexing.

🔹 Why lowercase + stopwords only?
Safe default; avoids over-tuning prematurely.


🔹 Why edismax?
It handles user-friendly queries, field boosting, and relevance tuning better than standard query parser