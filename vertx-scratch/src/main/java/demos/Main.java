package demos;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  @Override
  public void start() {

    vertx.setPeriodic(3000L, tick -> logger.info("Tick"));

    vertx.createHttpServer()
      .requestHandler(req -> {
        logger.info("HTTP request {}", req.method().name());
        req.response().end("Hello JokerConf");
      })
      .listen(8080)
      .onSuccess(ok -> logger.info("Server is running"))
      .onFailure(err -> logger.error("Woops", err));

  }

  public static void main(String[] args) {
    // var vertx = Vertx.vertx();
    // vertx.deployVerticle(new Main());

    Multi.createFrom().range(0, 100)
      .select().where(n -> n % 2 == 0)
      .subscribe().with(System.out::println);
  }
}
