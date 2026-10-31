public class Threads {
    public static void main() throws InterruptedException {

//        Thread lifeCycle Stages

        MyThread t1 = new MyThread();
        System.out.println("T1 thread stage : " + t1.getState());

        t1.start();
        System.out.println("T1 thread stage : " + t1.getState());

//        t1.run(); // it class the main thread execution itself as the run() is not responsible for the new thread creation request to OS via JVM , it is done through the Start method


        Thread t2 = new Thread( new MyRunnable());
        t2.start();

        //lamdas express way
        Thread t3 = new Thread(()-> {
            System.out.println("with lamda expression ");
            System.out.println("Thread t3 name : " + Thread.currentThread().getName() + "and its id is : " + Thread.currentThread().getId());
        });
        t3.start();

        System.out.println("T1 thread stage : " + t1.getState());


        t1.join();
        System.out.println("T1 thread stage : " + t1.getState());
        t2.join();
        t3.join();

        System.out.println(" ================ Now Thread Details ====================");
        System.out.println(Thread.currentThread().getName());
        System.out.println(Thread.currentThread().getId());

        Thread t4 = new Thread(()-> {
            while(!Thread.currentThread().isInterrupted()) {
                System.out.println(Thread.currentThread().getName());
            }
        });
        t4.start();
        Thread.sleep(500);
        t4.interrupt();
        if(t4.isInterrupted()) {
            System.out.println("Thread t4 is interrupted successfully");
        }

        Thread t5 = new Thread(()-> {
            while(true){
                System.out.println("Thread t5" +  " is running" + "As Daemon Thread 😈");
            }

        });

        t5.setDaemon(true);
        t5.start();

        Thread.sleep(2000);
        return;
    }
}

class MyThread extends Thread {
    @Override
    public void run() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("MyThread run is running");
    }
}

class MyRunnable implements Runnable {
    @Override
    public void run() {

        System.out.println("MyRunnable run is running");
    }
}