public class RaceCondition {
    static void main() {
        Counter c = new Counter();

       Thread t1 = new Thread(() -> {
           for (int i = 0; i < 1000; i++) {
               c.inc();
           }
       });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                c.inc();
            }
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(c.getCount());

    }
}

class Counter {
    private int count = 0;

   synchronized public void inc() {
        count++; // non atomic -- 3 operation.
    }
    public int getCount() {
        return count;
    }
}