# Console Chess

A dependency-free two-player chess game written in Java. It runs in a terminal and supports legal movement, check/checkmate, stalemate, castling, en passant, promotion, move history, and undo.

## Requirements

- Java 17 or newer
- Maven 3.8 or newer (optional)

## Run with Maven

```text
mvn compile exec:java
```

## Run the web version

Start the browser-based two-player game with Maven:

```text
mvn compile exec:java -Dexec.mainClass=com.example.chess.ChessWebServer
```

Open `http://localhost:8080`. The web game uses one shared board for all visitors.

## Run with `javac`

```text
javac -d out src/main/java/com/example/chess/ChessGame.java
java -cp out com.example.chess.ChessGame
```

## Commands

- Enter moves as `e2 e4` or `e2-e4`.
- Promote with `e7 e8 Q`, using `Q`, `R`, `B`, or `N`.
- `undo` takes back the last move.
- `help` shows the command format.
- `quit` exits the game.

Uppercase pieces are White and lowercase pieces are Black.

## Deploy to Render

The repository includes a `Dockerfile` for deploying the web version as a Render Web Service:

1. Push the project to GitHub and connect the repository in Render using **New > Web Service**.
2. Select the `main` branch, choose **Docker** as the runtime, and select the **Free** instance type.
3. Create the service. Render builds the Docker image and provides a public URL when deployment succeeds.

The service listens on Render's `PORT` environment variable. Free services can spin down when idle.
