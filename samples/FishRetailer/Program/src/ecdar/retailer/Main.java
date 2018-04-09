package ecdar.retailer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

public class Main {
    private static final String COIN = "coin";
    private static final String GARNISH = "garnish";
    private static final String TUNA = "tuna";

    private static Instant x;
    private static int loc;
    private static int free;
    private static boolean stepDone;
    private static TestHandler handler;


    public static void main(String[] args) throws IOException, InterruptedException {
        //handler = new RealTimeTestHandler(1000.0);
        handler = new SimulatedTimeTestHandler(1000.0);

        loc = 0;
        x = handler.resetTime();
        free = 0;

        while (loc >= 0) {
            stepDone = false;

            while (!stepDone) {
                update();
            }

            handler.onStepDone();
            //write("Debug: loc=" + loc + " x=" + getValue(x));
        }

        write("Debug: Done");
    }

    private static void update() throws IOException, InterruptedException {
        switch (loc) {
            case 0:
                if (inputReady()) {
                    if (read().equals(COIN)) {
                        free = 1;
                        x = handler.resetTime();
                        loc = 1;
                    } else loc = -1;
                } else if (free == 1 && getValue(x) > 5.0) {
                    write(GARNISH);
                    free = 0;
                } else stepDone = true;

                break;
            case 1:
                if (inputReady()) loc = -1;
                else if (getValue(x) > 2.0) {
                    write(TUNA);
                    loc = 0;
                } else stepDone = true;

                break;
        }
    }

    private static boolean inputReady() throws IOException {
        return handler.inputReady();
    }

    private static String read() {
        return handler.read();
    }

    private static void write(final String message) {
        handler.write(message);
    }

    private static double getValue(final Instant clock) {
        return handler.getValue(clock);
    }
}
