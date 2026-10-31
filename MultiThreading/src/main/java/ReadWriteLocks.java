import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLocks {
    public static void main(String[] args) throws InterruptedException {
        SharedResource sr = new SharedResource();
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

class SharedResource {
    private Integer value = 0;
    ReadWriteLock lock = new ReentrantReadWriteLock();
    Lock readLock = lock.readLock();
    Lock writeLock = lock.writeLock();


    void read() {
        readLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + ": read lock");
            Thread.sleep(1000);
            System.out.println("reading from shared resource : " + value);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            readLock.unlock();
        }

    }
    void write() {
        writeLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + ": write lock");
            Thread.sleep(1000);
            System.out.println("writing to shared resource : " + value);
            value++;
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            writeLock.unlock();
        }
    }
}