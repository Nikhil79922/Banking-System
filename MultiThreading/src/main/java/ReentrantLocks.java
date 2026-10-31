import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLocks {
    static void main() {
Thread t1 = new Thread(new Resources());
Thread t2 = new Thread(new Resources());
Thread t3 = new Thread(new Resources());
Thread t4 = new Thread(new Resources());
t1.start();
t2.start();
t3.start();
t4.start();
    }
}

class Resources implements Runnable{
    Lock lock = new ReentrantLock();

    void f1(){
        lock.lock();
        System.out.println("Lock acquired for Thread : " +  Thread.currentThread().getName());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
        System.out.println("Lock released for Thread : " +  Thread.currentThread().getName());
    }

    @Override
    public void run() {
        f1();
    }
}