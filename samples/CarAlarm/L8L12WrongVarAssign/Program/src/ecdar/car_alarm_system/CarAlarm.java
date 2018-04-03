package ecdar.car_alarm_system;

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
    private static List<String> inputs = Collections.synchronizedList(new ArrayList<>()); // use synchronized to be thread safe


    CarAlarm(){
    }

    void start() throws InterruptedException {
        new Thread(() -> {
            String line;
            final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                while ((line = reader.readLine()) != null) {
                    inputs.add(line);
                }
            } catch (IOException e) {
                write("Debug: " + e.getMessage());
            }
        }).start();

        clockX = Instant.now();
        location nextLocation = location.L0;
        while(!nextLocation.equals(location.Done)){
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
            delay();
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
                clockX = Instant.now();
                return location.L3;
            }
        } else {
            delay();
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
                clockX = Instant.now();
                return location.L3;
            }
        } else {
            delay();
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
                delay();
                return location.L3;
            }
        } else {
            write(OUTPUT_ARMED_ON);
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
        try {
            if(Duration.between(clockX, Instant.now()).toMillis() <= 3000) {
                if(inputReady()) {
                    if (read().equals(INPUT_UNLOCK)) {
                        clockX = Instant.now();
                        return location.L12;
                    }
                } else {
                    delay();
                    return location.L7;
                }
            } else {
                write(OUTPUT_SOUND_OFF);
                sound = false;
                return location.L8;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Error happend
        return location.Done;
    }

    private location L8(){
        try {
            if(Duration.between(clockX, Instant.now()).toMillis() <= 30000){
                if(inputReady()) {
                    if (read().equals(INPUT_UNLOCK)) {
                        clockX = Instant.now();
                        sound = true;
                        return location.L12;
                    }
                } else {
                    delay();
                    return location.L8;
                }
            } else if(Duration.between (clockX, Instant.now()).toMillis() > 30000){
                write(OUTPUT_FLASH_OFF);
                return location.L9;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Error happend
        return location.Done;
    }

    private location L9() {
        try {
            if (inputReady()) {
                String input = read();
                if (input.equals(INPUT_CLOSE)) {
                    clockX = Instant.now();
                    return location.L10;
                } else if (input.equals(INPUT_UNLOCK)) {
                    return location.L0;
                }
            }
            delay();
            return location.L9;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Error happend
        return location.Done;
    }

    private location L10(){
        write(OUTPUT_ARMED_ON);
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

    private location L14() throws InterruptedException {
        if (inputReady()) {
            String input = read();

            if(input.equals(INPUT_UNLOCK)) {
                clockX = Instant.now();
                return location.L11;
            } else if(input.equals(INPUT_OPEN)) {
                clockX = Instant.now();
                return location.L4;
            }
        } else {
            delay();
            return location.L14;
        }

        return location.Done;
    }



    private static boolean inputReady() {
        return !inputs.isEmpty();
    }

    private static String read() {
        final String input = inputs.get(0);
        inputs.remove(0);
        return input;
    }

    private static void write(final String... messages) {
        System.out.println(String.join("\n", messages));
    }

    private static double getValue(final Instant clock) {
        return Duration.between(clock, Instant.now()).toMillis() / 100.0;
    }

    private static void delay() throws InterruptedException {
        Thread.sleep(25);
    }
}
