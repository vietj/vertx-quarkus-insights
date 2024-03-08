package demo;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class VertxIsHere {

    @Inject
    Vertx vertx;

    void init(@Observes StartupEvent start) {

        Log.info("Plain Vert.x is possible");

        vertx.setTimer(5000, tick -> Log.info("🔔"));

        vertx.createNetServer()
                .connectHandler(socket -> {
                    Log.info("New TCP server connection");
                    socket.handler(socket::writeAndForget);
                    socket.exceptionHandler(err -> Log.info("TCP server error: " + err.getMessage()));
                    socket.endHandler(socket::closeAndForget);
                })
                .listenAndAwait(3000);
    }
}
