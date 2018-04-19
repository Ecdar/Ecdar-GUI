package ecdar.retailer;

import ecdar.sut.TestHandler;

import java.io.IOException;
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
        handler = TestHandler.createHandler(200.0, false);

        loc = 0;
        x = handler.resetTime();
        free = 0;

        handler.start(Main::runStep);
    }

    private static void runStep() throws IOException, InterruptedException {
        if (loc < 0) throw new RuntimeException("Loc is " + loc);

        stepDone = false;

        while (!stepDone) {
            update();
        }

        handler.onStepDone(Main::runStep);
    }

    private static void update() {
        switch (loc) {
            case 0:
                if (handler.inputReady()) {
                    if (handler.read().equals(COIN)) {
                        free = 1;
                        x = handler.resetTime();
                        loc = 1;
                    } else loc = -1;
                } else if (free == 1 && handler.getValue(x) > 5.0) {
                    handler.write(GARNISH);
                    free = 0;
                } else stepDone = true;

                break;
            case 1:
                if (handler.inputReady()) loc = -1;
                else if (handler.getValue(x) > 2.0) {
                    handler.write(TUNA);
                    loc = 0;
                } else stepDone = true;

                break;
        }
    }
}
