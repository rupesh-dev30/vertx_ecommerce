package com.rupesh.search.search_service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {

    vertx.deployVerticle(new HealthVerticle());
    vertx.deployVerticle(new KafkaConsumerVerticle());
    vertx.deployVerticle(new HttpVerticle());
    vertx.deployVerticle(new SearchHttpVerticle());

    startPromise.complete();
  }
}

