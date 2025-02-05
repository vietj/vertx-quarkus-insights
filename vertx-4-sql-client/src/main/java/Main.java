import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {

    logger.info("🚀 Starting a PostgreSQL container");

    PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:11-alpine")
      .withDatabaseName("postgres")
      .withUsername("postgres")
      .withPassword("vertx-in-action")
      .withInitScript("init.sql");

    postgreSQLContainer.start();

    logger.info("🚀 Starting Vert.x");

    Vertx vertx = Vertx.vertx();

    PgConnectOptions connectOptions = new PgConnectOptions()
      .setPort(postgreSQLContainer.getMappedPort(5432))
      .setHost(postgreSQLContainer.getHost())
      .setDatabase("postgres")
      .setUser("postgres")
      .setPassword("vertx-in-action");

    vertx.deployVerticle(new ApiVerticle(connectOptions)).onComplete(
      ok -> logger.info("✅ ApiVerticle was deployed successfully"),
      err -> logger.error("🔥 ApiVerticle deployment failed", err)
    );
  }
}
