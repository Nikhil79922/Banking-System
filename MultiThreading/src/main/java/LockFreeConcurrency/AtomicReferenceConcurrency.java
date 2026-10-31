package LockFreeConcurrency;

import java.util.concurrent.atomic.AtomicReference;

public class AtomicReferenceConcurrency {

    static void main() {
        SeatConcurrency seatConcurrency = new SeatConcurrency();

        Thread t1 = new Thread(() -> {
            seatConcurrency.bookSeat("Nikhil");
        });
        Thread t2 = new Thread(() -> {
            seatConcurrency.bookSeat("Shivam");
        });
        Thread t3 = new Thread(() -> {
            seatConcurrency.bookSeat("Prajesh");
        });
        Thread t4 = new Thread(() -> {
            seatConcurrency.bookSeat("Mandar");
        });
        Thread t5 = new Thread(() -> {
            seatConcurrency.bookSeat("Ashique");
        });
        Thread t6 = new Thread(() -> {
            seatConcurrency.bookSeat("Ayush");
        });
        Thread t7 = new Thread(() -> {
            seatConcurrency.bookSeat("Prasant");
        });

        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        t6.start();
        t7.start();

        try {
            t1.join();
            t2.join();
            t3.join();
            t4.join();
            t5.join();
            t6.join();
            t7.join();
        }catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("done");
    }
}


class SeatConcurrency {
    AtomicReference<String> seatStatus = new  AtomicReference<>("EMPTY");

    public boolean bookSeat(String name) {
        String currentValue = seatStatus.get();

        if (!currentValue.equals("EMPTY")) {
            System.out.println("Seat is Already Booked");
            return false;
        }

        Boolean allot = seatStatus.compareAndSet("EMPTY", name); //CAS :-compare and Set
        if (allot) {
            System.out.println("Seat is Booked for : " + name);
        }
        return allot;
    }
}