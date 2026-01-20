package com.rupesh.search.search_service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;

import java.util.HashMap;
import java.util.Map;

public class KafkaConsumerVerticle extends AbstractVerticle {

  private KafkaConsumer<String, String> consumer;
  private SolrClient solrClient;

  @Override
  public void start(Promise<Void> startPromise) {
    System.out.println("KafkaConsumerVerticle start() called");
    // 1. Initialize Solr Client
    this.solrClient = SolrClientProvider.getClient();

    // 2. Kafka Configuration
    Map<String, String> config = new HashMap<>();
    config.put(
      "bootstrap.servers",
      System.getenv().getOrDefault(
        "KAFKA_BOOTSTRAP_SERVERS",
        "ecommerce-kafka:9092"
      )
    );
    config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("group.id", "search-indexer-v2");
    config.put("auto.offset.reset", "earliest");
    config.put("enable.auto.commit", "true");

    // 3. Create Consumer
    this.consumer = KafkaConsumer.create(vertx, config);

    // 4. Set up Handler with executeBlocking
    consumer.handler(record -> {
      System.out.println("🔥 Consumed Kafka event: " + record.value());
      JsonObject event = new JsonObject(record.value());
      indexToSolr(event);
    });

    // 5. Subscribe and signal start
    consumer.subscribe("product-search-events")
      .onComplete(ar -> {
        if (ar.succeeded()) {
          System.out.println("Kafka Consumer started and subscribed to product-search-events");
          startPromise.complete();
        } else {
          startPromise.fail(ar.cause());
        }
      });
  }

  private void indexToSolr(JsonObject event) {
    JsonObject payload = event.getJsonObject("payload");

    // Move the blocking Solr operation to a Worker Thread
    vertx.executeBlocking(promise -> {
      try {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", payload.getString("id"));
        doc.addField("name", payload.getString("name"));
        doc.addField("description", payload.getString("description"));
        doc.addField("category", payload.getString("category"));
        doc.addField("price", payload.getDouble("price"));

        solrClient.add(doc);
        solrClient.commit();
        promise.complete();
      } catch (Exception e) {
        promise.fail(e);
      }
    }, res -> {
      if (res.failed()) {
        System.err.println("Failed to index document to Solr: " + res.cause().getMessage());
      }
    });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (consumer != null) {
      consumer.close().onComplete(stopPromise);
    } else {
      stopPromise.complete();
    }
  }
}
