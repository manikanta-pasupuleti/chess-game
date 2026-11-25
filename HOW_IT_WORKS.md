# How the Chess Game Works

This project is a two-player console chess game written in Java. The implementation is intentionally dependency-free: the Java standard library provides the input, collections, and record used by the game.

## Project Structure

```text
Chess/
|-- pom.xml
|-- README.md
|-- HOW_IT_WORKS.md
`-- src/main/java/com/example/chess/ChessGame.java
```

Most of the game is contained in `ChessGame.java`:

- `ChessGame` runs the input loop and displays messages.
- `Color` identifies White and Black.
- `Move` represents a requested or generated move.
- `Board` owns the position, move generation, rule checks, and state changes.
- `State` is an immutable snapshot used for simulation and undo.

## Starting the Program

The `main` method creates a `ChessGame` and calls `play()`:

```java
public static void main(String[] args) {
    new ChessGame().play();
}
```

The `play()` method repeatedly does the following:

1. Prints the current board.
2. Gets every legal move for the player whose turn it is.
3. Ends the game if there are no legal moves.
4. Reports check when the current king is attacked.
5. Reads a command or move from the terminal.
6. Finds the matching legal move and applies it.

A player can enter moves such as `e2 e4` or `e2-e4`. Commands are `help`, `undo`, and `quit`.

## Board Representation

The board is stored as a 64-element `char[]`:

```text
index = rank * 8 + file
```

The bottom-left square, `a1`, is index `0`. The top-right square, `h8`, is index `63`.

Piece characters use standard chess notation:

| Piece | White | Black |
| --- | --- | --- |
| King | `K` | `k` |
| Queen | `Q` | `q` |
| Rook | `R` | `r` |
| Bishop | `B` | `b` |
| Knight | `N` | `n` |
| Pawn | `P` | `p` |
| Empty square | `.` | `.` |

Uppercase pieces belong to White. Lowercase pieces belong to Black. The initial board is created in `Board.reset()`.

## How Moves Become Legal

`Board.legalMoves(Color side)` is the central rule-checking method.

1. `pseudoMoves` creates moves based on how each piece moves.
2. The board saves a `State` snapshot.
3. The candidate move is applied temporarily.
4. `isInCheck` checks whether the moving side's king is attacked.
5. The snapshot is restored.
6. Only moves that leave the king safe are returned.

This simulation approach prevents illegal moves such as moving into check, exposing the king to a rook, or moving a pinned piece incorrectly.

The input move is not applied directly. `findMatchingMove` compares the requested source and destination squares with the already validated legal move list. This keeps input parsing separate from chess rules.

## Piece Movement

The board generates movement in `pseudoMoves` by examining every piece belonging to the active player:

- Pawns move forward, move two squares from their starting rank, capture diagonally, promote on the last rank, and can capture en passant.
- Knights use their eight possible L-shaped offsets.
- Bishops slide diagonally until blocked.
- Rooks slide horizontally or vertically until blocked.
- Queens combine bishop and rook sliding directions.
- Kings move one square in any direction and may castle when the required conditions are met.

Sliding pieces stop at the first occupied square. They may capture an enemy piece there, but cannot move beyond it or capture a friendly piece.

## Special Rules

### Check, Checkmate, and Stalemate

`isInCheck` locates the king and asks whether the opponent can attack its square. After legal moves are generated:

- No legal moves plus check means checkmate.
- No legal moves without check means stalemate.
- At least one legal move means play continues.

### Castling

Castling is represented by a `Move` with `castle == true`. The board tracks whether each king and rook has moved. Castling is allowed only when:

- The king and appropriate rook have not moved.
- The squares between them are empty.
- The king is not currently in check.
- The king does not cross or land on an attacked square.

`applyUnchecked` moves the rook automatically when it applies a castling move.

### En Passant

After a pawn moves two squares, `enPassantTarget` stores the square it passed over. On the opponent's immediate turn, an eligible pawn may move to that target square. When applied, the captured pawn is removed from the adjacent square.

### Promotion

When a pawn reaches rank 1 or rank 8, the board generates four possible moves: queen, rook, bishop, and knight. The player selects one by entering a third value, for example:

```text
e7 e8 Q
```

## Applying and Undoing Moves

`Board.apply()` saves the current position before calling `applyUnchecked()`.

`applyUnchecked()`:

1. Removes the piece from its source square.
2. Handles en passant or castling changes.
3. Places the piece on the destination square.
4. Updates castling rights and the en passant target.
5. Switches the turn.

The `history` list stores `State` snapshots. `undo()` restores the most recent snapshot and removes it from the history. The snapshots include the board, turn, castling rights, and en passant target, so special-rule state is restored as well as piece positions.

## Running and Building

With Maven:

```text
mvn compile exec:java
```

Without Maven:

```text
javac -d out src/main/java/com/example/chess/ChessGame.java
java -cp out com.example.chess.ChessGame
```

The Maven project targets Java 17 or newer. The game has also been compiled and run with Java 25.

## Example Game Flow

```text
White to move > e2 e4
Black to move > e7 e5
White to move > g1 f3
Black to move > undo
```

`undo` restores the position before the last move and returns the turn to the player who made that move.
