package ecdar.car_alarm_system;

import ecdar.sut.TestHandler;

import java.io.IOException;
import java.time.Instant;

public class CarAlarm {
    //Inputs
    private static final String INPUT_CLOSE = "close";
    private static final String INPUT_OPEN = "open";
    private static final String INPUT_LOCK = "lock";
    private static final String INPUT_UNLOCK = "unlock";

    //Outputs
    private static final String OUTPUT_ARMED_OFF = "armedOff";
    private static final String OUTPUT_ARMED_ON = "armedOn";
    private static final String OUTPUT_FLASH_OFF = "flashOff";
    private static final String OUTPUT_FLASH_ON = "flashOn";
    private static final String OUTPUT_SOUND_OFF = "soundOff";
    private static final String OUTPUT_SOUND_ON = "soundOn";

    private enum location {L0, L1, L2, L3, L6, L9, L11, L12, L13, L5, L10, L4, Done}

    private Instant clockX;
    private boolean sound;
    private boolean alarmLocked;
    private static TestHandler handler;
    private static boolean stepDone;
    private location nextLocation;


    CarAlarm(){
    }

    void start() throws InterruptedException, IOException {
        handler = TestHandler.createHandler(100.0, false);

        clockX = handler.resetTime();
        nextLocation = location.L0;

        handler.start(this::runStep);
    }

    private void runStep() throws IOException, InterruptedException {
        stepDone = false;

        while (!stepDone) {
            update();
        }

        handler.onStepDone(this::runStep);
    }

    private void update() throws IOException, InterruptedException {

        switch (nextLocation) {
            case L0:
                nextLocation = L0();
                break;
            case L1:
                nextLocation = L1();
                break;
            case L2:
                nextLocation = L2();
                break;
            case L3:
                nextLocation = L3();
                break;
            case L4:
                nextLocation = L4();
                break;
            case L5:
                nextLocation = L5();
                break;
            case L6:
                nextLocation = L6();
                break;
            case L9:
                nextLocation = L9();
                break;
            case L10:
                nextLocation = L10();
                break;
            case L11:
                nextLocation = L11();
                break;
            case L12:
                nextLocation = L12();
                break;
            case L13:
                nextLocation = L13();
                break;
            case Done:
                System.exit(0);
                break;
            default:
                throw new RuntimeException("Location " + nextLocation.toString() + " not expected");
        }
    }



    private location L0() throws InterruptedException {
        if (inputReady()) {
            String input = read();

            if(input.equals(INPUT_CLOSE)) {
                return location.L1;
            } else if(input.equals(INPUT_LOCK)) {
                return location.L2;
            }
        } else {
            stepDone = true;
            return location.L0;
        }

        return location.Done;
    }

    private location L1() throws InterruptedException {
        if (inputReady()) {
            String input = read();

            if(input.equals(INPUT_OPEN)) {
                return location.L0;
            } else if(input.equals(INPUT_LOCK)) {
                clockX = handler.resetTime();
                return location.L3;
            }
        } else {
            stepDone = true;
            return location.L1;
        }

        return location.Done;
    }

    private location L2() throws InterruptedException {
        if (inputReady()) {
            String input = read();

            if(input.equals(INPUT_UNLOCK)) {
                return location.L0;
            } else if(input.equals(INPUT_CLOSE)) {
                clockX = handler.resetTime();
                return location.L3;
            }
        } else {
            stepDone = true;
            return location.L2;
        }

        return location.Done;
    }

    private location L3() throws InterruptedException {
        boolean timeOk = getValue(clockX) < 20.0;
        if (timeOk) {
            if (inputReady()) {
                final String input = read();

                if(input.equals(INPUT_UNLOCK)) {
                    return location.L1;
                } else if(input.equals(INPUT_OPEN)) {
                    return location.L2;
                }
            } else {
                stepDone = true;
                return location.L3;
            }
        } else {
            write(OUTPUT_ARMED_ON);
            alarmLocked = false;
            return location.L4;
        }
        return location.Done;
    }

    private location L4() {
        if (inputReady()) {
            String input = read();

            if(input.equals(INPUT_UNLOCK)) {
                clockX = handler.resetTime();
                return location.L5;
            } else if(input.equals(INPUT_OPEN)) {
                clockX = handler.resetTime();
                return location.L6;
            }
        } else if (!alarmLocked && getValue(clockX) > 400) {
            clockX = handler.resetTime();
            write(OUTPUT_ARMED_OFF);
            return location.L1;
        } else {
            stepDone = true;
            return location.L4;
        }

        return location.Done;
    }

    private location L5(){
        write(OUTPUT_ARMED_OFF);
        return location.L1;
    }

    private location L6(){
        write(OUTPUT_ARMED_OFF, OUTPUT_FLASH_ON, OUTPUT_SOUND_ON);
        sound = true;
        return location.L9;
    }

    private location L9() {
        if(getValue(clockX) <= 30) {
            if(inputReady()) {
                if (read().equals(INPUT_UNLOCK)) {
                    clockX = Instant.now();
                    return location.L10;
                }
            } else {
                stepDone = true;
                return location.L9;
            }
        } else {
            write(OUTPUT_SOUND_OFF);
            sound = false;
            return location.L11;
        }
        //Error happend
        return location.Done;
    }

    private location L10(){
        if(sound) {
            write(OUTPUT_SOUND_OFF);
            sound = false;
            return location.L10;
        } else {
            write(OUTPUT_FLASH_OFF);
            return location.L0;
        }
    }

    private location L11(){
        if (handler.getValue(clockX) <= 300.0){
            if(inputReady()) {
                if (read().equals(INPUT_UNLOCK)) {
                    clockX = Instant.now();
                    return location.L10;
                }
            } else {
                stepDone = true;
                return location.L11;
            }
        } else {
            write(OUTPUT_FLASH_OFF);
            return location.L12;
        }

        // Error happened
        return location.Done;
    }

    private location L12() {
        if (inputReady()) {
            String input = read();
            if (input.equals(INPUT_CLOSE)) {
                clockX = handler.resetTime();
                return location.L13;
            } else if (input.equals(INPUT_UNLOCK)) {
                return location.L0;
            }
        }
        stepDone = true;
        return location.L12;
    }

    private location L13(){
        write(OUTPUT_ARMED_ON);
        alarmLocked = true;
        return location.L4;
    }

    private static boolean inputReady() {
        return handler.inputReady();
    }

    private static String read() {
        return handler.read();
    }

    private static void write(final String... messages) {
        handler.write(messages);
    }

    private static double getValue(final Instant clock) {
        return handler.getValue(clock);
    }
}
