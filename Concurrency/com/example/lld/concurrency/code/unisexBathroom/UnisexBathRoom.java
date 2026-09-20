package code.unisexBathroom;

import java.util.concurrent.Semaphore;

public class UnisexBathRoom {
    private Gender inUseBy = null;
    int empInBath = 0;
    Semaphore maxEmps = new Semaphore(3);


    public void maleUseBathroom(String name) throws InterruptedException {
        synchronized (this){
            while(Gender.FEMALE == inUseBy){
                this.wait();
            }
            maxEmps.acquire();
            inUseBy = Gender.MALE;
            empInBath++;
        }
        useBathRoom(name);
        maxEmps.release();

        synchronized (this){
            empInBath--;
            if(empInBath == 0){
                inUseBy = null;
            }
            this.notifyAll();

        }
    }

    public void femaleUseBathroom(String name) throws InterruptedException {
        synchronized (this) {
            while (Gender.MALE == inUseBy) {
                wait();
            }
            maxEmps.acquire();
            inUseBy = Gender.FEMALE;
            empInBath++;
        }
        useBathRoom(name);
        maxEmps.release();

        synchronized (this){
            empInBath--;
            if(empInBath == 0){
                inUseBy = null;
            }
            this.notifyAll();
        }
    }

    private void useBathRoom(String name) throws InterruptedException {
        System.out.println("using bathroom" + name);
        Thread.sleep(1000);
        System.out.println("done using bathroom" + name);
    }

    enum Gender{
        MALE,
        FEMALE
    }
}
