package com.example.lld.tic_tac_toe;

import java.util.Scanner;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        TicTacToeGame game = new TicTacToeGame();
        game.playGame(new Scanner(System.in));
    }
}
