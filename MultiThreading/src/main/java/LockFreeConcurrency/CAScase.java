package LockFreeConcurrency;

import java.util.concurrent.atomic.AtomicReference;

public class CAScase {

    static void main() {
        LikeCounter likeCounter = new LikeCounter();

        for (int i = 0; i < 10; i++) {

            Thread t = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    likeCounter.like();
                }
            });

            t.start();
        }

        try{
            Thread.sleep(1000);
        }catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("Total Likes count : " + likeCounter.getLikeCount());
    }
}

class LikeCounter {
    AtomicReference<Integer> counter = new AtomicReference<>(0);

    public void like(){
        while(true){
            Integer value = counter.get();

            int increaseValue = value + 1;

            if(counter.compareAndSet(value, increaseValue)){
                return;
            }
            System.out.println("Conflict found retry.....");
        }
    }

    public Integer getLikeCount(){
        return counter.get();
    }
}