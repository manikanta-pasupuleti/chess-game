# Console Chess

A dependency-free, two-player chess game implemented in Java. Play locally in a terminal or use the browser-based interface.

**Live demo:** [Play Console Chess](https://chess-game-s2rr.onrender.com)

## Features

- Legal move validation, check, checkmate, and stalemate
- Castling, en passant, and pawn promotion
- Move history and undo
- Terminal and browser interfaces
- Java standard library only; no application dependencies

## Requirements

- Java 17 or later
- Maven 3.8 or later for Maven commands

## Play in the terminal

From the project directory, run:

```shell
mvn compile exec:java
```

Enter moves as `e2 e4` or `e2-e4`. Add a promotion piece when needed, for example `e7 e8 Q`. Use `help` to display the command format, `undo` to take back the previous move, and `quit` to exit.

White pieces are uppercase; Black pieces are lowercase.

### Run without Maven

```shell
javac -d out src/main/java/com/example/chess/ChessGame.java
java -cp out com.example.chess.ChessGame
```

## Play in a browser

Start the web server:

```shell
mvn compile exec:java -Dexec.mainClass=com.example.chess.ChessWebServer
```

Open [http://localhost:8080](http://localhost:8080). Select a piece and then a highlighted square to move. The browser game provides **one shared board for all visitors**; anyone who can access it can move pieces, undo, or start a new game.

## Deploy to Render

The repository includes a [Dockerfile](./Dockerfile) for deploying the web version as a Render Web Service:

1. Push the project to GitHub and sign in to [Render](https://render.com/).
2. Select **New > Web Service**, connect this repository, and choose the `main` branch.
3. Select **Docker** as the runtime and choose an instance plan.
4. Create the service. Render builds the image and provides a public URL when deployment succeeds.

The server listens on the `PORT` environment variable provided by Render. Free services may spin down when idle. The game state is stored in memory, so a service restart resets the board.

## Project layout

```text
.
├── Dockerfile
├── pom.xml
├── src
│   └── main
│       ├── java/com/example/chess
│       │   ├── ChessGame.java
│       │   └── ChessWebServer.java
│       └── resources/web/index.html
├── HOW_IT_WORKS.md
└── README.md
```
