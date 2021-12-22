package io.vertx.blog.first.route;

import io.vertx.blog.first.manager.WhiskyRepository;
import io.vertx.blog.first.manager.model.Whisky;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.stream.Collectors;

public class WhiskyRouteHandler {

  private final WhiskyRepository whiskyRepository;

  public WhiskyRouteHandler(WhiskyRepository whiskyRepository) {
    this.whiskyRepository = whiskyRepository;
  }

  public void addOne(RoutingContext routingContext) {
    final var bodyAsString = routingContext.getBodyAsString();
    if (bodyAsString == null) {
      routingContext.response()
          .setStatusCode(400).end();
    }

    final Whisky whisky = Json.decodeValue(bodyAsString, Whisky.class);

    whiskyRepository.save(whisky, asyncResult ->
        routingContext.response()
            .setStatusCode(201)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
            .end(Json.encodePrettily(whisky.setId(asyncResult.result()))));
  }

  public void getOne(RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      whiskyRepository.findById(id, asyncResult -> {
        if (asyncResult.succeeded()) {
          if (asyncResult.result() == null) {
            routingContext.response().setStatusCode(404).end();
            return;
          }
          Whisky whisky = new Whisky(asyncResult.result());
          routingContext.response()
              .setStatusCode(200)
              .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
              .end(Json.encodePrettily(whisky));
        } else {
          routingContext.response().setStatusCode(404).end();
        }
      });
    }
  }

  public void updateOne(RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    JsonObject json = routingContext.getBodyAsJson();
    if (id == null || json == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      whiskyRepository.updateById(id, json, asyncResult -> {
        if (asyncResult.failed()) {
          routingContext.response().setStatusCode(404).end();
        } else {
          routingContext.response()
              .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
              .end(Json.encodePrettily(
                  new Whisky(id, json.getString("name"), json.getString("origin"))));
        }
      });
    }
  }

  public void deleteOne(RoutingContext routingContext) {
    String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      whiskyRepository.deleteById(id, ar -> routingContext.response().setStatusCode(204).end());
    }
  }

  public void getAll(RoutingContext routingContext) {
    whiskyRepository.findAll(results -> {
      List<JsonObject> objects = results.result();
      List<Whisky> whiskies = objects.stream().map(Whisky::new).collect(Collectors.toList());
      routingContext.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
          .end(Json.encodePrettily(whiskies));
    });
  }
}
