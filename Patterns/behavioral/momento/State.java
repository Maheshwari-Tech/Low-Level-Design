package code.behavioral.momento;

public final class State {
    private final int height;
    private final int width;

    public State(int height, int width) {
        this.height = height;
        this.width = width;
    }

    public int height() {
        return height;
    }

    public int width() {
        return width;
    }

    @Override
    public String toString() {
        return "State[height=" + height + ", width=" + width + "]";
    }
}
