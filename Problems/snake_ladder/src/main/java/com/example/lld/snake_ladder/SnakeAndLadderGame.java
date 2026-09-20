/**
 * Snake and Ladder Game Implementation
 *
 * Design Choices:
 * - Board size is fixed at 100 squares for standard game.
 * - Snakes and ladders are predefined in maps for simplicity.
 * - Game uses turn-based play with dice rolls.
 * - Players move sequentially, with snakes and ladders applied immediately.
 * - Game ends when a player reaches exactly 100.
 * - Uses Scanner for user input to simulate interactive play.
 */
package com.example.lld.snake_ladder;

import java.util.*;

public class SnakeAndLadderGame {
    // Constants
    private static final int BOARD_SIZE = 100; // Standard board size

    // Game elements stored in maps for O(1) lookup
    private final Map<Integer, Integer> snakes; // Key: head position, Value: tail position
    private final Map<Integer, Integer> ladders; // Key: bottom position, Value: top position
    private final List<Player> players; // List of players in the game
    private final Random random; // Random number generator for dice

    /**
     * Constructor initializes the game board and collections.
     */
    public SnakeAndLadderGame() {
        this.snakes = new HashMap<>();
        this.ladders = new HashMap<>();
        this.players = new ArrayList<>();
        this.random = new Random();
        initializeBoard(); // Set up snakes and ladders
    }

    /**
     * Initializes the board with predefined snakes and ladders.
     * In a real game, these could be configurable.
     */
    private void initializeBoard() {
        // Snakes: positions where player slides down
        snakes.put(16, 6);
        snakes.put(47, 26);
        snakes.put(49, 11);
        snakes.put(56, 53);
        snakes.put(62, 19);
        snakes.put(64, 60);
        snakes.put(87, 24);
        snakes.put(93, 73);
        snakes.put(95, 75);
        snakes.put(98, 78);

        // Ladders: positions where player climbs up
        ladders.put(1, 38);
        ladders.put(4, 14);
        ladders.put(9, 31);
        ladders.put(21, 42);
        ladders.put(28, 84);
        ladders.put(36, 44);
        ladders.put(51, 67);
        ladders.put(71, 91);
        ladders.put(80, 100);
    }

    /**
     * Adds a player to the game.
     * @param name The name of the player
     */
    public void addPlayer(String name) {
        players.add(new Player(name));
    }

    /**
     * Main game loop that handles turns and game logic.
     * Players take turns rolling dice and moving.
     */
    public void play() {
        Scanner scanner = new Scanner(System.in);
        int currentPlayerIndex = 0;

        System.out.println("Welcome to Snake and Ladder Game!");
        System.out.println("First to reach 100 wins.\n");

        while (true) {
            Player currentPlayer = players.get(currentPlayerIndex);
            System.out.println(currentPlayer.getName() + "'s turn. Press enter to roll dice.");
            scanner.nextLine(); // Wait for user input

            int diceRoll = rollDice();
            System.out.println(currentPlayer.getName() + " rolled a " + diceRoll);

            int newPosition = currentPlayer.getPosition() + diceRoll;
            if (newPosition > BOARD_SIZE) {
                System.out.println("Roll exceeds board size. Stay at " + currentPlayer.getPosition());
            } else {
                currentPlayer.setPosition(newPosition);
                System.out.println(currentPlayer.getName() + " moved to " + newPosition);

                // Check for snakes
                if (snakes.containsKey(newPosition)) {
                    int snakeEnd = snakes.get(newPosition);
                    currentPlayer.setPosition(snakeEnd);
                    System.out.println("Oops! Snake at " + newPosition + ". Slide down to " + snakeEnd);
                }
                // Check for ladders
                else if (ladders.containsKey(newPosition)) {
                    int ladderEnd = ladders.get(newPosition);
                    currentPlayer.setPosition(ladderEnd);
                    System.out.println("Yay! Ladder at " + newPosition + ". Climb to " + ladderEnd);
                }

                // Check win condition
                if (currentPlayer.getPosition() == BOARD_SIZE) {
                    System.out.println(currentPlayer.getName() + " wins!");
                    break;
                }
            }

            // Move to next player
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        }

        scanner.close();
    }

    /**
     * Simulates rolling a six-sided die.
     * @return Random number between 1 and 6
     */
    private int rollDice() {
        return random.nextInt(6) + 1;
    }

    /**
     * Main method to start the game with sample players.
     */
    public static void main(String[] args) {
        SnakeAndLadderGame game = new SnakeAndLadderGame();
        game.addPlayer("Player 1");
        game.addPlayer("Player 2");
        game.play();
    }
}

class Player {
    private final String name;
    private int position;

    public Player(String name) {
        this.name = name;
        this.position = 0;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
