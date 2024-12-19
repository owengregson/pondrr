// Owen Gregson
// Artificial Intelligence
// TTT Checkpoint #3
// Dec 18, 2024

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

class StaticFileHandler implements HttpHandler {
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("js", "application/javascript");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("json", "application/json");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        String uri = t.getRequestURI().getPath();
        if (uri.equals("/")) {
            uri = "/visualizer.html";
        }

        if (uri.contains("..")) {
            String response = "403 Forbidden";
            t.sendResponseHeaders(403, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }

        Path filePath = Paths.get("." + uri).normalize();

        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            String extension = getFileExtension(filePath.getFileName().toString());
            String mimeType = MIME_TYPES.getOrDefault(extension, "application/octet-stream");
            byte[] response = Files.readAllBytes(filePath);
            t.getResponseHeaders().add("Content-Type", mimeType);
            t.sendResponseHeaders(200, response.length);
            OutputStream os = t.getResponseBody();
            os.write(response);
            os.close();
        } else {
            String response = "404 Not Found: " + uri;
            t.sendResponseHeaders(404, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot >= 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        } else {
            return "";
        }
    }
}