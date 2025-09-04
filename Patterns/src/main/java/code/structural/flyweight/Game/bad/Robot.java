package code.structural.flyweight.Game.bad;

public class Robot {
    private final int x; // 4B
    private final int y; // 4B
    private final String type;// 10B
    private final Sprites body; // 2d bitmap - 30KB


    public Robot(int x, int y, String type, Sprites body) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.body = body;
        // ~31KB
    }

    public int getY() {
        return y;
    }

    public int getX() {
        return x;
    }

    public String getType() {
        return type;
    }

    public Sprites getBody() {
        return body;
    }
}
