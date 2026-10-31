package ExecutorFrameworks;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Futures {

    static void main() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<Integer> f1 = executorService.submit(()->{return 2;});
        Future<Integer> f2 = executorService.submit(()->{return 3;});
        Future<Integer> f3 = executorService.submit(()->{
            try {
                Thread.sleep(3000);
            }catch (Exception e){
                e.printStackTrace();
            }

            return 4;});


        System.out.println(f3.get());  // now blocked

        System.out.println("Main thread not blocked yet");

//        System.out.println(f1.get());
//        System.out.println(f2.get());

        executorService.shutdown();
    }
}
