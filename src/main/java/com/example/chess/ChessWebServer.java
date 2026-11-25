package com.example.chess;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChessWebServer {
    private static final int MAX_REQUEST_BYTES = 2048;

    private final Game game = new Game();

    public static void main(String[] args) throws IOException {
        String portValue = System.getenv("PORT");
        int port = portValue == null || portValue.isBlank() ? 8080 : Integer.parseInt(portValue);
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        ChessWebServer application = new ChessWebServer();
        server.createContext("/", application::handle);
        server.start();
        System.out.println("Chess web app listening on port " + port);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (method.equals("GET") && path.equals("/")) {
                respond(exchange, 200, "text/html; charset=utf-8", readPage());
            } else if (method.equals("GET") && path.equals("/api/state")) {
                respond(exchange, 200, "application/json; charset=utf-8", game.stateJson());
            } else if (method.equals("POST") && path.equals("/api/move")) {
                respond(exchange, 200, "application/json; charset=utf-8", game.move(readForm(exchange)));
            } else if (method.equals("POST") && path.equals("/api/undo")) {
                respond(exchange, 200, "application/json; charset=utf-8", game.undo());
            } else if (method.equals("POST") && path.equals("/api/new")) {
                respond(exchange, 200, "application/json; charset=utf-8", game.newGame());
            } else {
                int status = path.startsWith("/api/") && !method.equals("GET") && !method.equals("POST") ? 405 : 404;
                respond(exchange, status, "application/json; charset=utf-8",
                        "{\"error\":\"" + (status == 405 ? "Method not allowed." : "Not found.") + "\"}");
            }
        } catch (IllegalArgumentException exception) {
            respond(exchange, 400, "application/json; charset=utf-8",
                    "{\"error\":\"" + jsonEscape(exception.getMessage()) + "\"}");
        } finally {
            exchange.close();
        }
    }

    private static byte[] readPage() throws IOException {
        try (InputStream input = ChessWebServer.class.getResourceAsStream("/web/index.html")) {
            if (input == null) {
                throw new IllegalStateException("The web page resource is missing.");
            }
            return input.readAllBytes();
        }
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readNBytes(MAX_REQUEST_BYTES + 1);
        if (body.length > MAX_REQUEST_BYTES) {
            throw new IllegalArgumentException("Request body is too large.");
        }

        Map<String, String> fields = new HashMap<>();
        String form = new String(body, StandardCharsets.UTF_8);
        for (String pair : form.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int separator = pair.indexOf('=');
            String key = separator < 0 ? pair : pair.substring(0, separator);
            String value = separator < 0 ? "" : pair.substring(separator + 1);
            fields.put(
                    URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return fields;
    }

    private static void respond(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        respond(exchange, status, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static final class Game {
        private ChessGame.Board board = new ChessGame.Board();

        synchronized String stateJson() {
            List<ChessGame.Move> legalMoves = board.legalMoves(board.turn());
            boolean inCheck = board.isInCheck(board.turn());
            String result = "";
            if (legalMoves.isEmpty()) {
                result = inCheck ? "Checkmate! " + board.turn().opposite() + " wins." : "Stalemate. Draw.";
            }

            StringBuilder json = new StringBuilder(2048);
            json.append("{\"board\":\"").append(board.position())
                    .append("\",\"turn\":\"").append(board.turn())
                    .append("\",\"inCheck\":").append(inCheck)
                    .append(",\"finished\":").append(legalMoves.isEmpty())
                    .append(",\"result\":\"").append(jsonEscape(result))
                    .append("\",\"legalMoves\":[");
            for (int i = 0; i < legalMoves.size(); i++) {
                ChessGame.Move move = legalMoves.get(i);
                if (i > 0) {
                    json.append(',');
                }
                json.append("{\"from\":\"").append(ChessGame.Move.name(move.from))
                        .append("\",\"to\":\"").append(ChessGame.Move.name(move.to)).append('"');
                if (move.promotion != '\0') {
                    json.append(",\"promotion\":\"")
                            .append(Character.toUpperCase(move.promotion)).append('"');
                }
                json.append('}');
            }
            return json.append("]}").toString();
        }

        synchronized String move(Map<String, String> fields) {
            String from = fields.get("from");
            String to = fields.get("to");
            if (from == null || to == null) {
                throw new IllegalArgumentException("Both from and to squares are required.");
            }
            String promotion = fields.getOrDefault("promotion", "");
            ChessGame.Move requested = ChessGame.Move.parse(
                    from + " " + to + (promotion.isEmpty() ? "" : " " + promotion));

            ChessGame.Move legalMove = findMove(board.legalMoves(board.turn()), requested);
            if (legalMove == null) {
                throw new IllegalArgumentException("Illegal move.");
            }
            board.apply(legalMove);
            return stateJson();
        }

        synchronized String undo() {
            if (!board.undo()) {
                throw new IllegalArgumentException("There is no move to undo.");
            }
            return stateJson();
        }

        synchronized String newGame() {
            board = new ChessGame.Board();
            return stateJson();
        }

        private static ChessGame.Move findMove(List<ChessGame.Move> legalMoves, ChessGame.Move requested) {
            for (ChessGame.Move move : legalMoves) {
                if (move.from != requested.from || move.to != requested.to) {
                    continue;
                }
                if (move.promotion == '\0' && requested.promotion == '\0') {
                    return move;
                }
                char selectedPromotion = requested.promotion == '\0' ? 'Q' : requested.promotion;
                if (move.promotion != '\0' && Character.toUpperCase(move.promotion) == selectedPromotion) {
                    return move;
                }
            }
            return null;
        }
    }
}
