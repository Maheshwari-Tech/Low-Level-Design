package com.example.lld.tic_tac_toe;

import java.util.Arrays;

public final class Board {
    private static final char EMPTY = ' ';

    private final char[][] cells;
    private final int size;

    public Board(int size) {
        if (size < 3) {
            throw new IllegalArgumentException("Board size must be at least 3");
        }
        this.size = size;
        this.cells = new char[size][size];
        for (char[] row : cells) {
            Arrays.fill(row, EMPTY);
        }
    }

    public boolean makeMove(int row, int column, char symbol) {
        if (!isValidMove(row, column)) {
            return false;
        }
        cells[row][column] = symbol;
        return true;
    }

    public boolean isValidMove(int row, int column) {
        return row >= 0 && row < size
                && column >= 0 && column < size
                && cells[row][column] == EMPTY;
    }

    public boolean isFull() {
        for (char[] row : cells) {
            for (char cell : row) {
                if (cell == EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean hasWinner() {
        return winningSymbol() != EMPTY;
    }

    public char winningSymbol() {
        for (int row = 0; row < size; row++) {
            if (sameNonEmptyRow(row)) {
                return cells[row][0];
            }
        }

        for (int column = 0; column < size; column++) {
            if (sameNonEmptyColumn(column)) {
                return cells[0][column];
            }
        }

        if (sameNonEmptyMainDiagonal()) {
            return cells[0][0];
        }
        if (sameNonEmptyAntiDiagonal()) {
            return cells[0][size - 1];
        }
        return EMPTY;
    }

    public void display() {
        System.out.print(render());
    }

    public String render() {
        StringBuilder result = new StringBuilder("Current Board:\n");
        String separator = "-".repeat(size * 4 - 3);
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                result.append(cells[row][column]);
                if (column < size - 1) {
                    result.append(" | ");
                }
            }
            result.append('\n');
            if (row < size - 1) {
                result.append(separator).append('\n');
            }
        }
        return result.toString();
    }

    public char[][] getBoard() {
        char[][] copy = new char[size][];
        for (int row = 0; row < size; row++) {
            copy[row] = Arrays.copyOf(cells[row], size);
        }
        return copy;
    }

    public int getSize() {
        return size;
    }

    private boolean sameNonEmptyRow(int row) {
        char candidate = cells[row][0];
        if (candidate == EMPTY) {
            return false;
        }
        for (int column = 1; column < size; column++) {
            if (cells[row][column] != candidate) {
                return false;
            }
        }
        return true;
    }

    private boolean sameNonEmptyColumn(int column) {
        char candidate = cells[0][column];
        if (candidate == EMPTY) {
            return false;
        }
        for (int row = 1; row < size; row++) {
            if (cells[row][column] != candidate) {
                return false;
            }
        }
        return true;
    }

    private boolean sameNonEmptyMainDiagonal() {
        char candidate = cells[0][0];
        if (candidate == EMPTY) {
            return false;
        }
        for (int index = 1; index < size; index++) {
            if (cells[index][index] != candidate) {
                return false;
            }
        }
        return true;
    }

    private boolean sameNonEmptyAntiDiagonal() {
        char candidate = cells[0][size - 1];
        if (candidate == EMPTY) {
            return false;
        }
        for (int row = 1; row < size; row++) {
            if (cells[row][size - 1 - row] != candidate) {
                return false;
            }
        }
        return true;
    }
}
