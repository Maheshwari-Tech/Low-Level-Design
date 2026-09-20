package code.structural.flyweight.Game.good;

import code.structural.flyweight.Game.bad.Sprites;

public class HumanoidRobot implements Robot{
    private final String type;
    private final Sprites sprites;

    public HumanoidRobot(String type, Sprites sprites) {
        this.type = type;
        this.sprites = sprites;
    }

    @Override
    public void display(int x, int y) {
        System.out.println("Displaying humanoid at " + x + ", " + y);
    }
}
