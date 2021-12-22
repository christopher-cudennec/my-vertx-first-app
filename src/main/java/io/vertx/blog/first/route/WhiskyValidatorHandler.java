package io.vertx.blog.first.route;

import io.vertx.blog.first.manager.WhiskyValidator;
import io.vertx.blog.first.manager.model.Whisky;
import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

public class WhiskyValidatorHandler implements Handler<RoutingContext> {

  private final WhiskyValidator validator;

  public WhiskyValidatorHandler(WhiskyValidator validator) {
    this.validator = validator;
  }

  @Override
  public void handle(RoutingContext context) {
    var body = context.getBody();
    if (body == null) {
      context.fail(400);
    }

    try {
      var whisky = Json.decodeValue(body, Whisky.class);
      if (validator.validate(whisky)) {
        context.next();
      }
      else {
        context.fail(422);
      }
    }
    catch (DecodeException e) {
      context.fail(400, e);
    }
  }
}
