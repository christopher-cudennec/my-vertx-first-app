package io.vertx.blog.first;

import static org.assertj.core.api.Assertions.assertThat;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.vertx.blog.first.manager.model.Whisky;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This is our JUnit test for our verticle. The test uses vertx-unit, so we declare a custom
 * runner.
 */
@ExtendWith(VertxExtension.class)
public class MyFirstVerticleTest {

  private Integer port;
  private static MongodProcess MONGO;
  private static final int MONGO_PORT = 12345;

  @BeforeAll
  public static void initialize() throws IOException {
    MongodStarter starter = MongodStarter.getDefaultInstance();

    MongodConfig mongodConfig = MongodConfig.builder()
        .version(Version.Main.PRODUCTION)
        .net(new Net(MONGO_PORT, Network.localhostIsIPv6()))
        .build();

    MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
    MONGO = mongodExecutable.start();
  }

  @AfterAll
  public static void shutdown() {
    MONGO.stop();
  }

  /**
   * Before executing our test, let's deploy our verticle.
   * <p/>
   * This method instantiates a new Vertx and deploy the verticle. Then, it waits in the verticle
   * has successfully completed its start sequence (thanks to `context.asyncAssertSuccess`).
   *
   * @param context the test context.
   */
  @BeforeEach
  public void setUp(VertxTestContext context) throws IOException {
    Vertx vertx = Vertx.vertx();

    // Let's configure the verticle to listen on the 'test' port (randomly picked).
    // We create deployment options and set the _configuration_ json object:
    ServerSocket socket = new ServerSocket(0);
    port = socket.getLocalPort();
    socket.close();

    DeploymentOptions options = new DeploymentOptions()
        .setConfig(new JsonObject()
            .put("http.port", port)
            .put("db_name", "whiskies-test")
            .put("connection_string", "mongodb://localhost:" + MONGO_PORT)
        );

    // We pass the options as the second parameter of the deployVerticle method.
    vertx.deployVerticle(MyFirstVerticle.class.getName(), options,
        context.succeedingThenComplete());
  }

  /**
   * This method, called after our test, just cleanup everything by closing the vert.x instance
   *
   * @param context the test context
   */
  @AfterEach
  public void tearDown(Vertx vertx, VertxTestContext context) {
    vertx.close(context.succeedingThenComplete());
  }

  /**
   * Let's ensure that our application behaves correctly.
   *
   * @param context the test context
   */
  @Test
  void testMyApplication(Vertx vertx, VertxTestContext context) {
    var webClient = WebClient.create(vertx);

    webClient.get(port, "localhost", "/")
        .send().onComplete(context.succeeding(response -> context.verify(() -> {
          assertThat(response.body().toString()).contains("Hello");
          context.completeNow();
        })));
  }

  @Test
  void checkThatTheIndexPageIsServed(Vertx vertx, VertxTestContext context) {
    var webClient = WebClient.create(vertx);

    webClient.get(port, "localhost", "/assets/index.html")
        .send().onComplete(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode()).isEqualTo(200);
          assertThat(response.getHeader("content-type")).isEqualTo("text/html;charset=UTF-8");

          assertThat(response.body().toString()).contains("<title>My Whisky Collection</title>");
          context.completeNow();
        })));
  }

  @Test
  void checkThatWeCanAdd(Vertx vertx, VertxTestContext context) {
    final var newWhisky = new Whisky("Jameson", "Ireland");

    var webClient = WebClient.create(vertx);
    webClient.post(port, "localhost", "/api/whiskies")
        .putHeader("content-type", "application/json")
        .sendJson(newWhisky)
        .onComplete(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode()).isEqualTo(201);
          assertThat(response.getHeader("content-type")).contains("application/json");

          var body = response.body();
          assertThat(body).isNotNull();
          final Whisky whisky = Json.decodeValue(body.toString(), Whisky.class);
          assertThat(whisky.getName()).isEqualTo("Jameson");
          assertThat(whisky.getOrigin()).isEqualTo("Ireland");
          assertThat(whisky.getId()).isNotNull();
          context.completeNow();
        })));
  }

  @Test
  void checkValidation(Vertx vertx, VertxTestContext context) {
    var webClient = WebClient.create(vertx);
    webClient.post(port, "localhost", "/api/whiskies")
        .putHeader("content-type", "application/json")
        .sendJson(new Whisky())
        .onComplete(context.succeeding(response -> context.verify(() -> {
          assertThat(response.statusCode()).isEqualTo(422);
          context.completeNow();
        })));
  }
}
