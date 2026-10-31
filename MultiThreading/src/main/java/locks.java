public final class Singleton {

    // Single shared instance
    private static volatile Singleton instance;

    // Private constructor prevents external object creation
    private Singleton() {
    }

    // Global access point
    public static Singleton getInstance() {

        // First check (without acquiring lock)
        if (instance == null) {

            synchronized (Singleton.class) {

                // Second check (inside synchronized block)
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }

        return instance;
    }

    // Test the Singleton
    public static void main(String[] args) {

        Singleton obj1 = Singleton.getInstance();
        Singleton obj2 = Singleton.getInstance();

        System.out.println(obj1 == obj2); // true
        System.out.println(obj1);
        System.out.println(obj2);
    }
}