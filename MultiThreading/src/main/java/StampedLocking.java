import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

public class StampedLocking {
    public static void main(String[] args) throws InterruptedException {
        SharedResource2 sr = new SharedResource2();
        Thread r1 = new Thread(() -> {
            sr.read();
        });
        Thread r2 = new Thread(() -> {
            sr.read();
        });
        Thread r3 = new Thread(() -> {
            sr.read();
        });

        Thread w1 = new Thread(() -> {
            sr.write();
        });
        Thread w2 = new Thread(() -> {
            sr.write();
        });
        Thread w3 = new Thread(() -> {
            sr.write();
        });


        r1.start();
        w1.start();
        r2.start();
        w2.start();
        r3.start();
        w3.start();

    }
}

class SharedResource2 {
    private Integer value = 0;
    StampedLock lock = new StampedLock();


    void read() {
       long stamp = lock.tryOptimisticRead();

       int currentValue = value;
       if(lock.validate(stamp) == false) {
           //fall over cases
           //pessimistic lock

           long readStamp = lock.readLock();
           try {
               System.out.println(Thread.currentThread().getName() + ": read lock");
               Thread.sleep(1000);
               System.out.println("reading from shared resource : " + value);
               return;
           }catch (Exception e){
               e.printStackTrace();
           }finally {
              lock.unlockRead(readStamp);
           }
       }
        System.out.println("reading from shared resource from current : " + currentValue);
       return;
    }
    void write() {
        long stamp = lock.writeLock();
        try {
            System.out.println(Thread.currentThread().getName() + ": write lock");
            Thread.sleep(1000);
            System.out.println("writing to shared resource : " + value);
            value++;
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            lock.unlockWrite(stamp);
        }
    }
}