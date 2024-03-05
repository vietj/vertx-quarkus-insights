package demo;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class ProductsApi {

    @Inject
    @ConfigProperty(name = "demo.dbcreate", defaultValue = "true")
    boolean dbCreate;

    @Inject
    PgPool pgPool;

    void init(@Observes StartupEvent start) {
        if (dbCreate) {
            prepareDB();
        }
    }

    private void prepareDB() {

        pgPool.query("""
                        CREATE TABLE Product
                        (
                            id    SERIAL PRIMARY KEY,
                            name  VARCHAR(255)   NOT NULL ,
                            price NUMERIC(19, 2) NOT NULL 
                        );
                        """)
                .executeAndAwait();

        Log.info("Database schema created");

        pgPool.preparedQuery("INSERT INTO Product(name, price) VALUES ($1, $2)")
                .executeBatchAndAwait(List.of(
                        Tuple.of("baguette", 1.05),
                        Tuple.of("eclairs", 4.50))
                );

        Log.info("Database populated with sample data");
    }

    void checkRouter(@Observes Router router) {
        router.get("/yolo").handler(ctx -> ctx.end("Yolo!"));
    }

    @Route(path = "/vertx-is-cool")
    public void sampleRoute(RoutingContext ctx) {
        ctx.response()
                .putHeader("Content-Type", "text/plain")
                .putHeader("X-AD", "Reading Vert.x in Action is a good idea")
                .end("Vert.x is cool!");
    }

    @Route(path = "/products", methods = Route.HttpMethod.GET)
    public Uni<JsonArray> listProducts() {

        Log.info("listProducts");

        return pgPool.query("SELECT * FROM Product")
                .mapping(Row::toJson)
                .execute()
                .onItem().transform(rows -> {
                    var products = new JsonArray();
                    rows.forEach(products::add);
                    return products;
                });
    }

    @Route(path = "/products", methods = Route.HttpMethod.POST)
    public Uni<JsonObject> createProduct(@Body JsonObject payload) {

        String name = payload.getString("name");
        BigDecimal price = new BigDecimal(payload.getString("price"));

        Log.info("createProduct name=" + name + ", price=" + price);

        return pgPool.preparedQuery("INSERT INTO Product(name, price) VALUES ($1, $2) RETURNING id")
                .execute(Tuple.of(name, price))
                .onItem().transform(rows -> rows.iterator().next().getInteger(0))
                .onItem().transform(id -> new JsonObject()
                        .put("id", id)
                        .put("name", name)
                        .put("price", price));
    }

    @Route(path = "/products/:id", methods = Route.HttpMethod.GET)
    public Uni<JsonObject> getProduct(@Param Integer id, RoutingContext ctx) {

        Log.info("getProduct id=" + id);

        return pgPool.preparedQuery("SELECT * FROM Product WHERE id=$1")
                .execute(Tuple.of(id))
                .onItem().transform(rows -> {
                    if (rows.iterator().hasNext()) {
                        return rows.iterator().next().toJson();
                    } else {
                        ctx.response().setStatusCode(404);
                        return new JsonObject();
                    }
                });
    }
}
