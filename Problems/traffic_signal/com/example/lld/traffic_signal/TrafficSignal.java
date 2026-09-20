package com.example.lld.traffic_signal;

/**
 * Traffic Signal System Implementation
 *
 * Design Choices:
 * - State pattern for signal states (Red, Yellow, Green).
 * - Fixed cycle: Red -> Green -> Yellow -> Red.
 * - Timer-based transitions using Thread.sleep for simulation.
 * - Console output for signal changes.
 */
public class TrafficSignal {
    private SignalState currentState;
    private int redDuration = 5000;    // 5 seconds
    private int greenDuration = 4000; // 4 seconds
    private int yellowDuration = 2000; // 2 seconds

    public TrafficSignal() {
        currentState = new RedState();
    }

    public void start() {
        System.out.println("Traffic signal started.");
        while (true) {
            currentState.display();
            try {
                Thread.sleep(currentState.getDuration());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            currentState = currentState.next();
        }
    }

    // State interface
    interface SignalState {
        void display();
        int getDuration();
        SignalState next();
    }

    // Red state
    class RedState implements SignalState {
        public void display() {
            System.out.println("Signal: RED - Stop!");
        }
        public int getDuration() {
            return redDuration;
        }
        public SignalState next() {
            return new GreenState();
        }
    }

    // Green state
    class GreenState implements SignalState {
        public void display() {
            System.out.println("Signal: GREEN - Go!");
        }
        public int getDuration() {
            return greenDuration;
        }
        public SignalState next() {
            return new YellowState();
        }
    }

    // Yellow state
    class YellowState implements SignalState {
        public void display() {
            System.out.println("Signal: YELLOW - Caution!");
        }
        public int getDuration() {
            return yellowDuration;
        }
        public SignalState next() {
            return new RedState();
        }
    }

    public static void main(String[] args) {
        TrafficSignal signal = new TrafficSignal();
        signal.start();
    }
}
