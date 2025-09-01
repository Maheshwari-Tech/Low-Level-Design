package com.mycompany.app.order_processing_system;

public enum Priority {
    URGENT(0),
    HIGH(1),
    NORMAL(2),
    LOW(3);

    final int rank;

    Priority(int rank){
        this.rank = rank;
    }
    public int getRank() {
        return rank;
    }
}
