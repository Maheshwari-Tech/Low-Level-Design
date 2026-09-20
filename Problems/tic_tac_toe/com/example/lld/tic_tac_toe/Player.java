package com.example.lld.tic_tac_toe;

public record Player(String name, char symbol) {
    public Player {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Player name is required");
        }
        if (Character.isWhitespace(symbol)) {
            throw new IllegalArgumentException("Player symbol cannot be whitespace");
        }
    }
}
