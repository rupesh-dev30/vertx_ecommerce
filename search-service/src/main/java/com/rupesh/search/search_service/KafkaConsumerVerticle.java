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
  private static final String TOPIC = "product-search-events";
  private final SolrClient solrClient = SolrClientProvider.getClient();

  @Override
  public void start(Promise<Void> startPromise) {

    System.out.println("🚀 KafkaConsumerVerticle started");

    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "ecommerce-kafka:29092");
    config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("group.id", "search-indexer-v3");
    config.put("auto.offset.reset", "earliest");
    config.put("enable.auto.commit", "false");

    consumer = KafkaConsumer.create(vertx, config);

//    consumer
//      .exceptionHandler(err ->
//        System.err.println("Kafka error: " + err.getMessage())
//      );
//
//    consumer
//      .partitionsAssignedHandler(p ->
//        System.out.println("Partitions assigned: " + p)
//      );

    consumer.handler(record -> {

      vertx.<String>executeBlocking(() -> {
        JsonObject json = new JsonObject(record.value());

        SolrInputDocument document = new SolrInputDocument();
        document.addField("id", json.getString("id"));
        document.addField("name", json.getString("name"));
        document.addField("description", json.getString("description"));
        document.addField("price", json.getDouble("price"));
        document.addField("category", json.getString("category"));
        document.addField("brand", json.getString("brand"));
        document.addField("created_at", json.getString("created_at"));

        solrClient.add(document);
        solrClient.commit();

        return json.getString("id");
      }).onSuccess(id -> {
        System.out.println("✅ Indexed product into Solr: " + id);
      }).onFailure(err -> {
        System.err.println("❌ Failed to index record");
        err.printStackTrace();
      });

    });

    consumer.subscribe(TOPIC)
      .onSuccess(v -> {
        System.out.println("Subscribed to " + TOPIC);
        startPromise.complete();
      })
      .onFailure(err -> {
        System.err.println("Subscribe failed: " + err.getMessage());
        startPromise.fail(err);
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
