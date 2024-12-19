// Owen Gregson
// Artificial Intelligence
// TTT Checkpoint #3
// Dec 18, 2024

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

public class NettyHandler {
    private HttpServer server;
    private Game game;
    private MoveHandler moveHandler;
    private RestartHandler restartHandler;
    private StatsHandler statsHandler;
    private final AtomicBoolean restartRequested = new AtomicBoolean(false);
    private TTT3.GameConfig config;

    public NettyHandler(Game game, int port, TTT3.GameConfig config) throws IOException {
        this.game = game;
        this.config = config;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        initializeContexts();
    }

    private void initializeContexts() {
        // Serve static files
        server.createContext("/", new StaticFileHandler());

        // Handle game state requests
        GameStateHandler gameStateHandler = new GameStateHandler(game);
        server.createContext("/gamestate", gameStateHandler);

        // Handle move submissions
        this.moveHandler = new MoveHandler();
        server.createContext("/move", this.moveHandler);

        // restarting
        this.restartHandler = new RestartHandler(restartRequested);
        server.createContext("/restart", this.restartHandler);

        // choosing side
        server.createContext("/choose-x", new ChooseSideHandler(Player.X));
        server.createContext("/choose-o", new ChooseSideHandler(Player.O));

        // game stats
        this.statsHandler = new StatsHandler(game);
		server.createContext("/stats", this.statsHandler);

		server.createContext("/sounds", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String requested = exchange.getRequestURI().getPath().replace("/sounds/", "");
                java.nio.file.Path filePath = Paths.get(requested);
                if (Files.exists(filePath)) {
                    byte[] fileBytes = Files.readAllBytes(filePath);
                    exchange.getResponseHeaders().set("Content-Type", "audio/wav");
                    exchange.sendResponseHeaders(200, fileBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(fileBytes);
                    }
                } else {
                    exchange.sendResponseHeaders(404, -1);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        });
    }

    public void start() {
        server.setExecutor(null);
        server.start();
        System.out.println("Server started at http://localhost:" + server.getAddress().getPort() + "/");
    }

    public void stop(int delay) {
        server.stop(delay);
        System.out.println("Server stopped.");
    }

    public int getSelectedMove() {
        return moveHandler.getSelectedMove();
    }

    public void resetSelectedMove() {
        moveHandler.resetSelectedMove();
    }

    public void waitForRestart() {
        System.out.println("Waiting for restart request...");
        synchronized (restartRequested) {
            while (!restartRequested.get()) {
                try {
                    restartRequested.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        restartRequested.set(false);
        System.out.println("Restart request received.");
    }

    static class RestartHandler implements HttpHandler {
        private final AtomicBoolean restartRequested;

        public RestartHandler(AtomicBoolean restartRequested) {
            this.restartRequested = restartRequested;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                synchronized (restartRequested) {
                    restartRequested.set(true);
                    restartRequested.notifyAll();
                }
                String response = "Restart requested.";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    class ChooseSideHandler implements HttpHandler {
        private final Player chosenSide;
        public ChooseSideHandler(Player chosenSide) {
            this.chosenSide = chosenSide;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                if (config.isPlayingAgainstAI && config.humanPlayer == null) {
                    config.humanPlayer = chosenSide;
                    Player aiSide = config.humanPlayer.other();
                    if (aiSide == Player.X && config.aiPlayerX == null) {
                        config.aiPlayerX = new AIPlayer(Player.X);
                    } else if (aiSide == Player.O && config.aiPlayerO == null) {
                        config.aiPlayerO = new AIPlayer(Player.O);
                    }
                    game.humanPlayer = config.humanPlayer;
                    game.aiPlayerX = config.aiPlayerX;
                    game.aiPlayerO = config.aiPlayerO;
                    game.aiSide = aiSide;
                    System.out.println("Human player chosen: " + chosenSide);
                }
                String response = "Side chosen: " + chosenSide;
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class StatsHandler implements HttpHandler {
        private final Game game;
        public StatsHandler(Game game) {
            this.game = game;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String response = game.stats();
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
}
