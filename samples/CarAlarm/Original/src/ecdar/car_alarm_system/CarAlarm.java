package ecdar.car_alarm_system;

import ecdar.sut.TestHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private enum location {L0, L1, L2, L3, L4, L7, L8, L9, L10, L11, L12, L14, Done}

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
            case L7:
                nextLocation = L7();
                break;
            case L8:
                nextLocation = L8();
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
            case L14:
                nextLocation = L14();
                break;
            case Done:
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
            return location.L14;
        }
        return location.Done;
    }

    private location L4(){
        write(OUTPUT_ARMED_OFF, OUTPUT_FLASH_ON, OUTPUT_SOUND_ON);
        sound = true;
        return location.L7;
    }

    private location L7() {
        if(getValue(clockX) <= 30) {
            if(inputReady()) {
                if (read().equals(INPUT_UNLOCK)) {
                    clockX = Instant.now();
                    return location.L12;
                }
            } else {
                stepDone = true;
                return location.L7;
            }
        } else {
            write(OUTPUT_SOUND_OFF);
            sound = false;
            return location.L8;
        }
        //Error happend
        return location.Done;
    }

    private location L8(){
        if (handler.getValue(clockX) <= 300.0){
            if(inputReady()) {
                if (read().equals(INPUT_UNLOCK)) {
                    clockX = Instant.now();
                    return location.L12;
                }
            } else {
                stepDone = true;
                return location.L8;
            }
        } else {
            write(OUTPUT_FLASH_OFF);
            return location.L9;
        }

        // Error happened
        return location.Done;
    }

    private location L9() {
        if (inputReady()) {
            String input = read();
            if (input.equals(INPUT_CLOSE)) {
                clockX = handler.resetTime();
                return location.L10;
            } else if (input.equals(INPUT_UNLOCK)) {
                return location.L0;
            }
        }
        stepDone = true;
        return location.L9;
    }

    private location L10(){
        write(OUTPUT_ARMED_ON);
        alarmLocked = true;
        return location.L14;
    }

    private location L11(){
        write(OUTPUT_ARMED_OFF);
        return location.L1;
    }

    private location L12(){
        if(sound) {
            write(OUTPUT_SOUND_OFF);
            sound = false;
            return location.L12;
        } else {
            write(OUTPUT_FLASH_OFF);
            return location.L0;
        }
    }

    private location L14() {
        if (inputReady()) {
            String input = read();

            if(input.equals(INPUT_UNLOCK)) {
                clockX = handler.resetTime();
                return location.L11;
            } else if(input.equals(INPUT_OPEN)) {
                clockX = handler.resetTime();
                return location.L4;
            }
        } else if (!alarmLocked && getValue(clockX) > 400) {
            clockX = handler.resetTime();
            write(OUTPUT_ARMED_OFF);
            return location.L1;
        } else {
            stepDone = true;
            return location.L14;
        }

        return location.Done;
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
