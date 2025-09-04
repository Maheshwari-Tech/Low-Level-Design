package code.structural.flyweight.Game.good;

import code.structural.flyweight.Game.bad.Sprites;

import java.util.HashMap;
import java.util.Map;

public class RoboticFactory {

    static Map<String, Robot> robotMap = new HashMap<>();

    public static Robot createRobot(String type){
        if(!robotMap.containsKey(type)){

            switch (type){
                case "dog":
                {
                    Sprites humanSprite = new Sprites();
                    Robot robot = new HumanoidRobot(type, humanSprite);
                    robotMap.put(type, robot);
                    break;
                }
                case "human":{
                    Sprites dogSprite = new Sprites();
                    Robot robot = new RoboticDog(type, dogSprite);
                    robotMap.put(type, robot);
                    break;
                }
                default:
                    throw new RuntimeException("invalid");
            }
        }
        return robotMap.get(type);
    }
}
