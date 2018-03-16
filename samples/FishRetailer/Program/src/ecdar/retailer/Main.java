package ecdar.retailer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Scanner;

public class Main {
    private static final String COIN = "coin";
    private static final String GARNISH = "garnish";
    private static final String TUNA = "tuna";


    public static void main(String[] args) {
        int loc = 0;
        Instant x = Instant.now();
        int free = 0;

        try {
            while (loc >= 0) {
                switch (loc) {
                    case 0:
                        if (inputReady()) {
                            if (read().equals(COIN)) {
                                free = 1;
                                x = Instant.now();
                                loc = 1;
                            } else loc = -1;
                        } else if (free == 1 && getValue(x) > 5.0) {
                            write(GARNISH);
                            free = 0;
                        } else delay();

                        break;
                    case 1:
                        if (inputReady()) loc = -1;
                        else if (getValue(x) > 2.0) {
                            write(TUNA);
                            loc = 0;
                        } else delay();

                        break;
                }

                //write("Debug: loc=" + loc + " x=" + getValue(x));
            }

            write("Debug: Done");
        } catch (Exception e) {
            write("Debug: " + e.getMessage());
        }
    }

    private static boolean inputReady() throws IOException {
        System.out.flush();
        return System.in.available() != 0;
    }

    private static String read() {
        System.out.flush();
        return new Scanner(System.in).nextLine();
    }

    private static void write(final String message) {
        System.out.println(message);
        System.out.flush();
    }

    private static double getValue(final Instant clock) {
        return Duration.between(clock, Instant.now()).toMillis() / 1000.0;
    }

    private static void delay() throws InterruptedException {
        Thread.sleep(250);
    }
}
