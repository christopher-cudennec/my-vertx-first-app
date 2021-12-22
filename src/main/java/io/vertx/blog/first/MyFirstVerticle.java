package io.vertx.blog.first;

import io.vertx.blog.first.manager.WhiskyRepository;
import io.vertx.blog.first.manager.WhiskyValidator;
import io.vertx.blog.first.route.WhiskyRouteHandler;
import io.vertx.blog.first.route.WhiskyValidatorHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a verticle. A verticle is a _Vert.x component_. This verticle is implemented in Java, but
 * you can implement them in JavaScript, Groovy or even Ruby.
 */
public class MyFirstVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MyFirstVerticle.class);

  private WhiskyRepository whiskyRepository;

  /**
   * This method is called when the verticle is deployed. It creates a HTTP server and registers a
   * simple request handler.
   * <p/>
   * Notice the `listen` method. It passes a lambda checking the port binding result. When the HTTP
   * server has been bound on the port, it call the `complete` method to inform that the starting
   * has completed. Else it reports the error.
   *
   * @param fut the future
   */
  @Override
  public void start(Promise<Void> fut) {

    // Create a Mongo client
    var mongo = MongoClient.createShared(vertx, config());
    whiskyRepository = new WhiskyRepository(mongo);

    whiskyRepository.createSomeData(
        (nothing) -> startWebApp(
            (http) -> completeStartup(http, fut)
        ), fut);
  }

  private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
    // Create a router object.
    Router mainRouter = Router.router(vertx);

    // Bind "/" to our hello message.
    mainRouter.route("/").handler(routingContext -> routingContext.response()
        .putHeader("content-type", "text/html")
        .end("<h1>Hello from my first Vert.x 3 application</h1>"));

    mainRouter.route("/assets/*").handler(StaticHandler.create("assets"));

    var validator = new WhiskyValidator();
    var whiskyValidatorHandler = new WhiskyValidatorHandler(validator);

    var whiskyRouteHandler = new WhiskyRouteHandler(whiskyRepository);

    mainRouter.get("/api/whiskies").handler(whiskyRouteHandler::getAll);
    mainRouter.route("/api/whiskies*").handler(BodyHandler.create());
    mainRouter.post("/api/whiskies").handler(whiskyValidatorHandler)
        .handler(whiskyRouteHandler::addOne);
    mainRouter.get("/api/whiskies/:id").handler(whiskyRouteHandler::getOne);
    mainRouter.put("/api/whiskies/:id").handler(whiskyValidatorHandler)
        .handler(whiskyRouteHandler::updateOne);
    mainRouter.delete("/api/whiskies/:id").handler(whiskyRouteHandler::deleteOne);

    // Create the HTTP server and pass the "accept" method to the request handler.
    vertx
        .createHttpServer()
        .requestHandler(mainRouter)
        .listen(
            // Retrieve the port from the configuration,
            // default to 8080.
            config().getInteger("http.port", 8080),
            next
        );

    LOGGER.info("Routes:");
    mainRouter.getRoutes().stream().map(Route::getPath).sorted().distinct().forEach(
        path -> LOGGER.info(" {}", path));
  }

  private void completeStartup(AsyncResult<HttpServer> http, Promise<Void> promise) {
    if (http.succeeded()) {
      promise.complete();
    } else {
      promise.fail(http.cause());
    }
  }

  @Override
  public void stop() {
    whiskyRepository.close();
  }


}
