// Owen Gregson
// Artificial Intelligence
// TTT Checkpoint #3
// Dec 18, 2024

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

class GameStateHandler implements HttpHandler {
    private Game game;

    public GameStateHandler(Game game) {
        this.game = game;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        try {
            String gameState = game.board.toString();
            String response = "{\"gameState\": \"" + gameState + "\","
                    + "\"isPlayingAgainstAI\": " + game.isPlayingAgainstAI + ","
                    + "\"currentPlayer\": \"" + game.nextPlayer + "\","
                    + "\"aiPlayerX\": " + (game.aiPlayerX != null ? "\"X\"" : "\"None\"") + ","
                    + "\"aiPlayerO\": " + (game.aiPlayerO != null ? "\"O\"" : "\"None\"") + ","
                    + "\"isGameOver\": " + game.isGameOver + ","
                    + "\"winner\": " + (game.winner != null ? "\"" + game.winner + "\"" : "null") + ","
                    + "\"winningLine\": " + getWinningLinePositions() + "}";
            t.getResponseHeaders().add("Content-Type", "application/json");
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*"); // For CORS
            t.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes("UTF-8"));
            os.close();
        } catch (Exception e) {
            String response = "Internal Server Error: " + e.getMessage();
            t.sendResponseHeaders(500, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            e.printStackTrace();
        }
    }

    private String getWinningLinePositions() {
        return (game.winningLine == null) ? "null" : game.winningLine.toString().replace("{", "[").replace("}", "]");
    }
}
