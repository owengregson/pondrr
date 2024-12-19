// Owen Gregson
// Artificial Intelligence
// TTT Checkpoint #3
// Dec 18, 2024

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;

class MoveHandler implements HttpHandler {
    private int selectedMove = -1;

    @Override
    public void handle(HttpExchange t) throws IOException {
        try {
            if ("POST".equalsIgnoreCase(t.getRequestMethod())) {
                InputStream is = t.getRequestBody();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String requestBody = sb.toString();
                String[] params = requestBody.split("=");
                if (params.length == 2 && params[0].equals("move")) {
                    selectedMove = Integer.parseInt(params[1]);
                    System.out.println("Received move: " + selectedMove);
                }
                t.getResponseHeaders().add("Access-Control-Allow-Origin", "*"); // For CORS
                t.sendResponseHeaders(200, -1);
            } else if ("OPTIONS".equalsIgnoreCase(t.getRequestMethod())) {
                t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                t.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                t.sendResponseHeaders(204, -1);
            } else {
                t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                t.sendResponseHeaders(405, -1);
            }
            t.close();
        } catch (Exception e) {
            String response = "Internal Server Error: " + e.getMessage();
            t.sendResponseHeaders(500, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            e.printStackTrace();
        }
    }

    public int getSelectedMove() {
        return selectedMove;
    }

    public void resetSelectedMove() {
        selectedMove = -1;
    }
}
