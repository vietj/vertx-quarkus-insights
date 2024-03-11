import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.pgclient.PgBuilder;
import io.vertx.sqlclient.*;
import io.vertx.pgclient.PgConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class ApiVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(ApiVerticle.class);

  private final PgConnectOptions connectOptions;

  private SqlClient client;

  public ApiVerticle(PgConnectOptions connectOptions) {
    this.connectOptions = connectOptions;
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

    client = PgBuilder.pool(builder -> builder
      .with(poolOptions)
      .connectingTo(connectOptions)
      .using(vertx));

    Router router = Router.router(vertx);

    BodyHandler bodyHandler = BodyHandler.create();
    router.post().handler(bodyHandler);

    router.get("/products").respond(rc -> listProducts());
    router.get("/products/:id").respond(this::getProduct);
    router.post("/products").respond(this::createProduct);

    Future<HttpServer> httpStart = vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080)
      .onSuccess(s -> logger.info("HTTP server listening on port 8080"));

    httpStart.onComplete(s -> startPromise.complete(), startPromise::fail);
  }

  private Future<JsonArray> listProducts() {
    logger.info("listProducts");

    return client.query("SELECT * FROM Product")
      .collecting(Collectors.mapping(row -> row.toJson(), Collectors.toList()))
      .execute()
      .map(sqlResult -> new JsonArray(sqlResult.value()))
      .otherwise(new JsonArray());
  }

  private Future<JsonObject> createProduct(RoutingContext rc) {
    logger.info("createProduct");

    JsonObject json = rc.body().asJsonObject();
    String name;
    BigDecimal price;

    try {
      requireNonNull(json, "The incoming JSON document cannot be null");
      name = requireNonNull(json.getString("name"), "The product name cannot be null");
      price = new BigDecimal(json.getString("price"));
    } catch (Throwable err) {
      logger.error("Could not extract values", err);
      return Future.failedFuture(err);
    }

    return client
      .preparedQuery("INSERT INTO Product(name, price) VALUES ($1, $2) RETURNING id")
      .execute(Tuple.of(name, price))
      .map(rowSet -> {
        Row row = rowSet.iterator().next();
        return new JsonObject()
          .put("id", row.getInteger("id"))
          .put("name", name)
          .put("price", price);
      })
      .onFailure(err -> logger.error("Woops", err));
  }

  private Future<JsonObject> getProduct(RoutingContext rc) {
    logger.info("getProduct");
    Long id = Long.valueOf(rc.pathParam("id"));

    return client
      .preparedQuery("SELECT * FROM Product WHERE id=$1")
      .execute(Tuple.of(id))
      .map(rows -> {
        RowIterator<Row> iterator = rows.iterator();
        if (iterator.hasNext()) {
          return iterator.next().toJson();
        } else {
          return new JsonObject();
        }
      })
      .onFailure(err -> logger.error("Woops", err));
  }
}

