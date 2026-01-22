package com.rupesh.search.search_service;

import io.vertx.core.AbstractVerticle;

public class HttpVerticle extends AbstractVerticle {

  @Override
  public void start() {
    vertx.createHttpServer()
      .requestHandler(req -> {
        req.response()
          .putHeader("content-type", "text/plain")
          .end("Hello from Search Service!");
      })
      .listen(8888, ar -> {
        if (ar.succeeded()) {
          System.out.println("HTTP server started on port 8888");
        } else {
          System.err.println("Failed to start HTTP server");
        }
      });
  }
}
