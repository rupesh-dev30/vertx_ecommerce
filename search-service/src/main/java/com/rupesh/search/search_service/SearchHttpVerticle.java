package com.rupesh.search.search_service;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;

public class SearchHttpVerticle extends AbstractVerticle {

  private final SolrClient solrClient = SolrClientProvider.getClient();

  @Override
  public void start(Promise<Void> startPromise) {

    vertx.createHttpServer().requestHandler(req -> {

      if (!"/api/search".equals(req.path())) {
        req.response().setStatusCode(404).end();
        return;
      }

      String q = req.getParam("q");
      if (q == null || q.isBlank()) {
        req.response().setStatusCode(400).end("Missing query param q");
        return;
      }

      String category = req.getParam("category");
      String sort = req.getParam("sort");
      int page = Integer.parseInt(req.getParam("page", "0"));
      int size = Integer.parseInt(req.getParam("size", "10"));

      vertx.executeBlocking(promise -> {
        try {
          SolrQuery query = new SolrQuery();
          query.setQuery("text_all:" + q);
          query.setStart(page * size);
          query.setRows(size);

          if (category != null) {
            query.addFilterQuery("category:" + category);
          }

          if ("price_asc".equals(sort)) {
            query.setSort("price", SolrQuery.ORDER.asc);
          } else if ("price_desc".equals(sort)) {
            query.setSort("price", SolrQuery.ORDER.desc);
          }

          QueryResponse response = solrClient.query(query);

          JsonArray docs = new JsonArray();
          response.getResults().forEach(doc -> {
            docs.add(JsonObject.mapFrom(doc));
          });

          JsonObject result = new JsonObject()
            .put("total", response.getResults().getNumFound())
            .put("page", page)
            .put("size", size)
            .put("items", docs);

          promise.complete(result);

        } catch (Exception e) {
          promise.fail(e);
        }
      }).onSuccess(res -> {
        req.response()
          .putHeader("content-type", "application/json")
          .end(res.toString());
      }).onFailure(err -> {
        req.response().setStatusCode(500).end("Search failed");
      });

    }).listen(8888);

    System.out.println("🔍 Search API started on port 8888");
  }
}
