# Console Chess

A dependency-free two-player chess game written in Java. It runs in a terminal and supports legal movement, check/checkmate, stalemate, castling, en passant, promotion, move history, and undo.

## Requirements

- Java 17 or newer
- Maven 3.8 or newer (optional)

## Run with Maven

```text
mvn compile exec:java
```

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
