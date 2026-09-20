package com.example.lld.tic_tac_toe;

import java.util.Objects;
import java.util.Scanner;

public final class TicTacToeGame {
    private final Board board;
    private final Player player1;
    private final Player player2;

    private Player currentPlayer;
    private Player winner;
    private GameStatus status = GameStatus.IN_PROGRESS;

    public TicTacToeGame() {
        this(3, new Player("Player 1", 'X'), new Player("Player 2", 'O'));
    }

    public TicTacToeGame(int boardSize, Player player1, Player player2) {
        this.board = new Board(boardSize);
        this.player1 = Objects.requireNonNull(player1, "player1");
        this.player2 = Objects.requireNonNull(player2, "player2");
        if (player1.symbol() == player2.symbol()) {
            throw new IllegalArgumentException("Players must use different symbols");
        }
        this.currentPlayer = player1;
    }

    public boolean playMove(int row, int column) {
        if (status != GameStatus.IN_PROGRESS || !board.makeMove(row, column, currentPlayer.symbol())) {
            return false;
        }

        if (board.hasWinner()) {
            status = GameStatus.WON;
            winner = currentPlayer;
        } else if (board.isFull()) {
            status = GameStatus.DRAW;
        } else {
            currentPlayer = currentPlayer.equals(player1) ? player2 : player1;
        }
        return true;
    }

    public void playGame() {
        playGame(new Scanner(System.in));
    }

    public void playGame(Scanner scanner) {
        Objects.requireNonNull(scanner, "scanner");
        System.out.println("=== Tic-Tac-Toe Game ===");

        while (status == GameStatus.IN_PROGRESS) {
            board.display();
            System.out.printf("%s's turn (%s)%n", currentPlayer.name(), currentPlayer.symbol());
            System.out.printf("Enter row and column (0-%d): ", board.getSize() - 1);

            if (!scanner.hasNextInt()) {
                System.out.println("Expected two integer coordinates.");
                scanner.nextLine();
                continue;
            }
            int row = scanner.nextInt();
            if (!scanner.hasNextInt()) {
                System.out.println("Expected a column coordinate.");
                scanner.nextLine();
                continue;
            }
            int column = scanner.nextInt();

            if (!playMove(row, column)) {
                System.out.println("Invalid move. Try again.");
            }
        }

        board.display();
        if (status == GameStatus.WON) {
            System.out.println(winner.name() + " wins!");
        } else {
            System.out.println("It's a draw!");
        }
    }

    public Board getBoard() {
        return board;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public Player getWinner() {
        return winner;
    }

    public GameStatus getStatus() {
        return status;
    }
}
