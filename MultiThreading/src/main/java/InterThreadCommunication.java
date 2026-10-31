public class InterThreadCommunication {
    static void main() {
        Box b = new Box();
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                try {
                    Thread.sleep(100);
                    b.producer(i);
                } catch (InterruptedException e) {
                }
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                try {
                    Thread.sleep(70);
                    b.consumer();
                } catch (InterruptedException e) {
                }
            }
        });

        t1.start();
        t2.start();
    }
}

class Box {
   volatile Integer item;
   volatile Boolean flag = false;

   synchronized void producer(Integer val) throws InterruptedException { // dead lock
        while(flag == true) {
            wait();
        }
        item = val;
        flag = true;
        System.out.println("Producer produce : " + item);
        notify();
    }

    synchronized void consumer() throws InterruptedException { // dead lock if consumer runs first
        while(flag == false) {
            wait();
        }
        System.out.println("Consumer is consume : " + item);
        flag = false;
        item = null;
        notify();
    }
}