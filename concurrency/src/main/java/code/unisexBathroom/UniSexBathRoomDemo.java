package code.unisexBathroom;

import java.util.Random;

public class UniSexBathRoomDemo {

    private static Random random = new Random();
    public static void main(String[] args) throws InterruptedException {
        runTests();
    }

    public static void runTests() throws InterruptedException{

        UnisexBathRoom unisexBathRoom = new UnisexBathRoom();
        int count = 0;
        while(count < 10){
            System.out.println(count);
            ++count;
            int gender = random.nextInt(2);
            if(gender == 1){
                femaleThread(count, unisexBathRoom);
            }
            else{
                maleThread(count, unisexBathRoom);
            }
        }
    }

    private static void femaleThread(final int count, final UnisexBathRoom unisexBathRoom) {
        System.out.println("Girl Arrives");

        Thread female = new Thread(() -> {
            try {
                unisexBathRoom.femaleUseBathroom("girl :" + count);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }});
        female.start();
    }

    private static void maleThread(final int count, final UnisexBathRoom unisexBathRoom) {
        System.out.println("Boy Arrives");

        Thread male = new Thread(() -> {
            try {
                unisexBathRoom.maleUseBathroom("boy :" + count);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }});
        male.start();
    }
}
