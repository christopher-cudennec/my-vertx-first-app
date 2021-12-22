package io.vertx.blog.first.manager;

import io.vertx.blog.first.manager.model.Whisky;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import java.util.List;

public class WhiskyRepository {

  public static final String COLLECTION = "whiskies";
  private final MongoClient mongo;

  public WhiskyRepository(MongoClient mongo) {
    this.mongo = mongo;
  }

  public void createSomeData(Handler<AsyncResult<Void>> next, Promise<Void> promise) {
    Whisky bowmore = new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay");
    Whisky talisker = new Whisky("Talisker 57° North", "Scotland, Island");
    System.out.println(bowmore.toJson());

    // Do we have data in the collection ?
    countAll(count -> {
      if (count.succeeded()) {
        if (count.result() == 0) {
          // no whiskies, insert data
          mongo.insert(COLLECTION, bowmore.toJson(), ar -> {
            if (ar.failed()) {
              promise.fail(ar.cause());
            } else {
              mongo.insert(COLLECTION, talisker.toJson(), ar2 -> {
                if (ar2.failed()) {
                  promise.fail(ar2.cause());
                } else {
                  next.handle(Future.succeededFuture());
                }
              });
            }
          });
        } else {
          next.handle(Future.succeededFuture());
        }
      } else {
        // report the error
        promise.fail(count.cause());
      }
    });
  }

  public void close() {
    mongo.close();
  }

  public void countAll(
      Handler<AsyncResult<Long>> resultHandler) {
    mongo.count(COLLECTION, new JsonObject(), resultHandler);
  }

  public void save(Whisky whisky, Handler<AsyncResult<String>> resultHandler) {
    mongo.insert(COLLECTION, whisky.toJson(), resultHandler);
  }

  public void findById(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
    mongo.findOne(COLLECTION, new JsonObject().put("_id", id), null, resultHandler);
  }

  public void updateById(String id, JsonObject newValue, Handler<AsyncResult<JsonObject>> resultHandler) {
    mongo.findOneAndUpdate(COLLECTION,
        new JsonObject().put("_id", id), // Select a unique document
        // The update syntax: {$set, the json object containing the fields to update}
        new JsonObject().put("$set", newValue), resultHandler
    );
  }

  public void deleteById(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
    mongo.findOneAndDelete(COLLECTION, new JsonObject().put("_id", id), resultHandler);
  }

  public void findAll(
      Handler<AsyncResult<List<JsonObject>>> resultHandler) {
    mongo.find(COLLECTION, new JsonObject(), resultHandler);
  }
}
