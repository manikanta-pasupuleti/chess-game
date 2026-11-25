package com.example.chess;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class ChessGame {
    private final Board board = new Board();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new ChessGame().play();
    }

    private void play() {
        System.out.println("\n=== Console Chess ===");
        System.out.println("Enter moves like e2 e4. Type help for commands.\n");

        while (true) {
            board.print();
            Color side = board.turn;
            List<Move> legalMoves = board.legalMoves(side);
            if (legalMoves.isEmpty()) {
                if (board.isInCheck(side)) {
                    System.out.println("Checkmate! " + side.opposite() + " wins.");
                } else {
                    System.out.println("Stalemate. Draw.");
                }
                return;
            }
            if (board.isInCheck(side)) {
                System.out.println(side + " is in check.");
            }

            System.out.print(side + " to move > ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye.");
                return;
            }
            if (input.equalsIgnoreCase("help")) {
                System.out.println("Move format: e2 e4 or e2-e4. Promotion: e7 e8 Q. Commands: undo, help, quit.");
                continue;
            }
            if (input.equalsIgnoreCase("undo")) {
                if (board.undo()) {
                    System.out.println("Last move undone.");
                } else {
                    System.out.println("There is no move to undo.");
                }
                continue;
            }

            try {
                Move requested = Move.parse(input);
                Move legalMove = findMatchingMove(legalMoves, requested);
                if (legalMove == null) {
                    System.out.println("Illegal move. Try a different move or type help.");
                    continue;
                }
                board.apply(legalMove);
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
            }
        }
    }

    private Move findMatchingMove(List<Move> legalMoves, Move requested) {
        for (Move move : legalMoves) {
            if (move.from == requested.from && move.to == requested.to
                    && (requested.promotion == '\0' || Character.toUpperCase(move.promotion) == requested.promotion)) {
                return move;
            }
        }
        return null;
    }

    enum Color {
        WHITE, BLACK;

        Color opposite() {
            return this == WHITE ? BLACK : WHITE;
        }

        @Override
        public String toString() {
            return this == WHITE ? "White" : "Black";
        }
    }

    static final class Move {
        final int from;
        final int to;
        final char promotion;
        final boolean castle;
        final boolean enPassant;

        Move(int from, int to) {
            this(from, to, '\0', false, false);
        }

        Move(int from, int to, char promotion, boolean castle, boolean enPassant) {
            this.from = from;
            this.to = to;
            this.promotion = promotion;
            this.castle = castle;
            this.enPassant = enPassant;
        }

        static Move parse(String input) {
            String[] parts = input.toLowerCase().replace('-', ' ').trim().split("\\s+");
            if (parts.length < 2 || parts.length > 3) {
                throw new IllegalArgumentException("Use moves like e2 e4, optionally followed by Q, R, B, or N.");
            }
            int from = square(parts[0]);
            int to = square(parts[1]);
            char promotion = '\0';
            if (parts.length == 3) {
                if (parts[2].length() != 1 || "qrbn".indexOf(parts[2].charAt(0)) < 0) {
                    throw new IllegalArgumentException("Promotion must be Q, R, B, or N.");
                }
                promotion = Character.toUpperCase(parts[2].charAt(0));
            }
            return new Move(from, to, promotion, false, false);
        }

        static int square(String value) {
            if (value.length() != 2 || value.charAt(0) < 'a' || value.charAt(0) > 'h'
                    || value.charAt(1) < '1' || value.charAt(1) > '8') {
                throw new IllegalArgumentException("Square must look like e4.");
            }
            return (value.charAt(1) - '1') * 8 + value.charAt(0) - 'a';
        }

        static String name(int square) {
            return "" + (char) ('a' + square % 8) + (char) ('1' + square / 8);
        }
    }

    static final class Board {
        private final char[] squares = new char[64];
        private final List<State> history = new ArrayList<>();
        private Color turn = Color.WHITE;
        private boolean whiteKingMoved;
        private boolean blackKingMoved;
        private boolean whiteRookKingMoved;
        private boolean whiteRookQueenMoved;
        private boolean blackRookKingMoved;
        private boolean blackRookQueenMoved;
        private int enPassantTarget = -1;

        Board() {
            reset();
        }

        void reset() {
            String[] rows = {"rnbqkbnr", "pppppppp", "........", "........", "........", "........", "PPPPPPPP", "RNBQKBNR"};
            for (int rank = 0; rank < 8; rank++) {
                for (int file = 0; file < 8; file++) {
                    squares[rank * 8 + file] = rows[7 - rank].charAt(file);
                }
            }
        }

        void print() {
            System.out.println();
            System.out.println("    a b c d e f g h");
            System.out.println("  +-----------------+");
            for (int rank = 7; rank >= 0; rank--) {
                System.out.print((rank + 1) + " | ");
                for (int file = 0; file < 8; file++) {
                    System.out.print(squares[rank * 8 + file] + " ");
                }
                System.out.println("| " + (rank + 1));
            }
            System.out.println("  +-----------------+");
            System.out.println("    a b c d e f g h");
            System.out.println();
        }

        String position() {
            return new String(squares);
        }

        Color turn() {
            return turn;
        }

        List<Move> legalMoves(Color side) {
            List<Move> moves = pseudoMoves(side, true);
            List<Move> legal = new ArrayList<>();
            for (Move move : moves) {
                State state = save();
                applyUnchecked(move);
                if (!isInCheck(side)) {
                    legal.add(move);
                }
                restore(state);
            }
            return legal;
        }

        private List<Move> pseudoMoves(Color side, boolean includeCastling) {
            List<Move> moves = new ArrayList<>();
            for (int square = 0; square < 64; square++) {
                char piece = squares[square];
                if (piece == '.' || colorOf(piece) != side) {
                    continue;
                }
                char type = Character.toUpperCase(piece);
                if (type == 'P') addPawnMoves(moves, square, side);
                if (type == 'N') addKnightMoves(moves, square, side);
                if (type == 'B' || type == 'R' || type == 'Q') addSlidingMoves(moves, square, side, type);
                if (type == 'K') addKingMoves(moves, square, side, includeCastling);
            }
            return moves;
        }

        private void addPawnMoves(List<Move> moves, int from, Color side) {
            int direction = side == Color.WHITE ? 1 : -1;
            int file = from % 8;
            int rank = from / 8;
            int oneForward = from + direction * 8;
            if (inBounds(oneForward) && squares[oneForward] == '.') {
                addPawnMove(moves, from, oneForward, side);
                int twoForward = from + direction * 16;
                if ((side == Color.WHITE ? rank == 1 : rank == 6) && squares[twoForward] == '.') {
                    moves.add(new Move(from, twoForward));
                }
            }
            for (int fileDelta : new int[]{-1, 1}) {
                int targetFile = file + fileDelta;
                int target = from + direction * 8 + fileDelta;
                if (targetFile < 0 || targetFile > 7 || !inBounds(target)) continue;
                if (squares[target] != '.' && colorOf(squares[target]) != side) {
                    addPawnMove(moves, from, target, side);
                } else if (target == enPassantTarget) {
                    moves.add(new Move(from, target, '\0', false, true));
                }
            }
        }

        private void addPawnMove(List<Move> moves, int from, int to, Color side) {
            int rank = to / 8;
            if (rank == 0 || rank == 7) {
                for (char promotion : new char[]{'Q', 'R', 'B', 'N'}) {
                    moves.add(new Move(from, to, side == Color.WHITE ? promotion : Character.toLowerCase(promotion), false, false));
                }
            } else {
                moves.add(new Move(from, to));
            }
        }

        private void addKnightMoves(List<Move> moves, int from, Color side) {
            int file = from % 8;
            int rank = from / 8;
            int[][] offsets = {{1, 2}, {2, 1}, {-1, 2}, {-2, 1}, {1, -2}, {2, -1}, {-1, -2}, {-2, -1}};
            for (int[] offset : offsets) {
                int targetFile = file + offset[0];
                int targetRank = rank + offset[1];
                if (targetFile >= 0 && targetFile < 8 && targetRank >= 0 && targetRank < 8) addIfAvailable(moves, from, targetRank * 8 + targetFile, side);
            }
        }

        private void addSlidingMoves(List<Move> moves, int from, Color side, char type) {
            int[][] directions = type == 'B' ? new int[][]{{1, 1}, {-1, 1}, {1, -1}, {-1, -1}}
                    : type == 'R' ? new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}
                    : new int[][]{{1, 1}, {-1, 1}, {1, -1}, {-1, -1}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] direction : directions) {
                int file = from % 8 + direction[0];
                int rank = from / 8 + direction[1];
                while (file >= 0 && file < 8 && rank >= 0 && rank < 8) {
                    int target = rank * 8 + file;
                    if (squares[target] == '.') moves.add(new Move(from, target));
                    else {
                        if (colorOf(squares[target]) != side) moves.add(new Move(from, target));
                        break;
                    }
                    file += direction[0];
                    rank += direction[1];
                }
            }
        }

        private void addKingMoves(List<Move> moves, int from, Color side, boolean includeCastling) {
            int file = from % 8;
            int rank = from / 8;
            for (int fileDelta = -1; fileDelta <= 1; fileDelta++) {
                for (int rankDelta = -1; rankDelta <= 1; rankDelta++) {
                    if (fileDelta == 0 && rankDelta == 0) continue;
                    int targetFile = file + fileDelta;
                    int targetRank = rank + rankDelta;
                    if (targetFile >= 0 && targetFile < 8 && targetRank >= 0 && targetRank < 8) addIfAvailable(moves, from, targetRank * 8 + targetFile, side);
                }
            }
            if (!includeCastling || isInCheck(side)) return;
            int home = side == Color.WHITE ? 0 : 7;
            boolean kingMoved = side == Color.WHITE ? whiteKingMoved : blackKingMoved;
            boolean kingRookMoved = side == Color.WHITE ? whiteRookKingMoved : blackRookKingMoved;
            boolean queenRookMoved = side == Color.WHITE ? whiteRookQueenMoved : blackRookQueenMoved;
            if (!kingMoved && !kingRookMoved && squares[home * 8 + 5] == '.' && squares[home * 8 + 6] == '.'
                    && squares[home * 8 + 7] == (side == Color.WHITE ? 'R' : 'r')
                    && !isSquareAttacked(home * 8 + 5, side.opposite()) && !isSquareAttacked(home * 8 + 6, side.opposite())) {
                moves.add(new Move(home * 8 + 4, home * 8 + 6, '\0', true, false));
            }
            if (!kingMoved && !queenRookMoved && squares[home * 8 + 1] == '.' && squares[home * 8 + 2] == '.' && squares[home * 8 + 3] == '.'
                    && squares[home * 8] == (side == Color.WHITE ? 'R' : 'r')
                    && !isSquareAttacked(home * 8 + 3, side.opposite()) && !isSquareAttacked(home * 8 + 2, side.opposite())) {
                moves.add(new Move(home * 8 + 4, home * 8 + 2, '\0', true, false));
            }
        }

        private void addIfAvailable(List<Move> moves, int from, int to, Color side) {
            if (squares[to] == '.' || colorOf(squares[to]) != side) moves.add(new Move(from, to));
        }

        boolean isInCheck(Color side) {
            char king = side == Color.WHITE ? 'K' : 'k';
            int kingSquare = -1;
            for (int square = 0; square < 64; square++) if (squares[square] == king) kingSquare = square;
            return kingSquare < 0 || isSquareAttacked(kingSquare, side.opposite());
        }

        private boolean isSquareAttacked(int target, Color bySide) {
            for (Move move : pseudoMoves(bySide, false)) if (move.to == target) return true;
            return false;
        }

        void apply(Move move) {
            history.add(save());
            applyUnchecked(move);
        }

        private void applyUnchecked(Move move) {
            char piece = squares[move.from];
            Color side = colorOf(piece);
            char captured = squares[move.to];
            squares[move.from] = '.';
            if (move.enPassant) squares[move.to + (side == Color.WHITE ? -8 : 8)] = '.';
            if (move.castle) {
                int rank = move.to / 8;
                if (move.to % 8 == 6) {
                    squares[rank * 8 + 5] = squares[rank * 8 + 7];
                    squares[rank * 8 + 7] = '.';
                } else {
                    squares[rank * 8 + 3] = squares[rank * 8];
                    squares[rank * 8] = '.';
                }
            }
            squares[move.to] = move.promotion == '\0' ? piece : move.promotion;
            updateRights(piece, move.from, move.to, captured);
            enPassantTarget = Math.abs(move.to - move.from) == 16 ? (move.to + move.from) / 2 : -1;
            turn = turn.opposite();
        }

        private void updateRights(char piece, int from, int to, char captured) {
            if (piece == 'K') whiteKingMoved = true;
            if (piece == 'k') blackKingMoved = true;
            if (piece == 'R' && from == 0) whiteRookQueenMoved = true;
            if (piece == 'R' && from == 7) whiteRookKingMoved = true;
            if (piece == 'r' && from == 56) blackRookQueenMoved = true;
            if (piece == 'r' && from == 63) blackRookKingMoved = true;
            if (captured == 'R' && to == 0) whiteRookQueenMoved = true;
            if (captured == 'R' && to == 7) whiteRookKingMoved = true;
            if (captured == 'r' && to == 56) blackRookQueenMoved = true;
            if (captured == 'r' && to == 63) blackRookKingMoved = true;
        }

        boolean undo() {
            if (history.isEmpty()) return false;
            restore(history.remove(history.size() - 1));
            return true;
        }

        private State save() {
            return new State(squares.clone(), turn, whiteKingMoved, blackKingMoved, whiteRookKingMoved, whiteRookQueenMoved,
                    blackRookKingMoved, blackRookQueenMoved, enPassantTarget);
        }

        private void restore(State state) {
            System.arraycopy(state.squares, 0, squares, 0, squares.length);
            turn = state.turn;
            whiteKingMoved = state.whiteKingMoved;
            blackKingMoved = state.blackKingMoved;
            whiteRookKingMoved = state.whiteRookKingMoved;
            whiteRookQueenMoved = state.whiteRookQueenMoved;
            blackRookKingMoved = state.blackRookKingMoved;
            blackRookQueenMoved = state.blackRookQueenMoved;
            enPassantTarget = state.enPassantTarget;
        }

        private static boolean inBounds(int square) {
            return square >= 0 && square < 64;
        }

        private static Color colorOf(char piece) {
            return Character.isUpperCase(piece) ? Color.WHITE : Color.BLACK;
        }
    }

    private record State(char[] squares, Color turn, boolean whiteKingMoved, boolean blackKingMoved,
                         boolean whiteRookKingMoved, boolean whiteRookQueenMoved, boolean blackRookKingMoved,
                         boolean blackRookQueenMoved, int enPassantTarget) {
    }
}
