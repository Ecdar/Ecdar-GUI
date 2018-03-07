package ecdar.car_alarm_system;

import java.io.*;
import java.time.Duration;
import java.time.Instant;

public class CarAlarm {
    //Inputs
    public static final String INPUT_CLOSE = "close";
    public static final String INPUT_OPEN = "open";
    public static final String INPUT_LOCK = "lock";
    public static final String INPUT_UNLOCK = "unlock";

    //Outputs
    public static final String OUTPUT_ARMED_OFF = "armedOff";
    public static final String OUTPUT_ARMED_ON = "armedOn";
    public static final String OUTPUT_FLASH_OFF = "flashOff";
    public static final String OUTPUT_FLASH_ON = "flashOn";
    public static final String OUTPUT_SOUND_OFF = "soundOff";
    public static final String OUTPUT_SOUND_ON = "soundOn";

    enum location {L0, L1, L2, L3, L4, L5, L6, L7, L8, L9, L10, L11, L12, L14, Done}

    Instant clockX;
    boolean sound;
    BufferedReader reader;
    InputStream inputStream;


    CarAlarm(){
        inputStream = System.in;
        reader = new BufferedReader(new InputStreamReader(inputStream));
        start();
    }

    void start(){
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
                case L5:
                    nextLocation = L5();
                    break;
                case L6:
                    nextLocation = L6();
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
                    return;
            }
        }
        return;
    }

    private location L0() {
        try {
            String input = reader.readLine();

            if(input.equals(INPUT_CLOSE)) {
                return location.L1;
            } else if(input.equals(INPUT_LOCK)) {
                return location.L2;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return location.Done;
    }

    private location L1(){
        try {
            String input = reader.readLine();

            if(input.equals(INPUT_OPEN)) {
                return location.L0;
            } else if(input.equals(INPUT_LOCK)) {
                clockX = Instant.now();
                return location.L3;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return location.Done;
    }

    private location L2() {
        try {
            String input = reader.readLine();

            if(input.equals(INPUT_UNLOCK)) {
                return location.L0;
            } else if(input.equals(INPUT_CLOSE)) {
                clockX = Instant.now();
                return location.L3;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return location.Done;
    }

    private location L3() {
        try {
            if (Duration.between(clockX, Instant.now()).toMillis() <= 2000) {
                if (System.in.available() != 0) {
                    final String input = reader.readLine();

                    if(input.equals(INPUT_UNLOCK)) {
                        return location.L1;
                    } else if(input.equals(INPUT_OPEN)) {
                        return location.L2;
                    }
                } else {
                    Thread.sleep(25);
                    return location.L3;
                }
            } else {
                System.out.println(OUTPUT_ARMED_ON);
                return location.L14;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return location.Done;
    }

    private location L4(){
        if(Duration.between(clockX, Instant.now()).toMillis() <= 100){
            System.out.println(OUTPUT_ARMED_OFF);
            return location.L5;
        }
        return location.Done;
    }

    private location L5(){
        System.out.println(OUTPUT_FLASH_ON);
        return location.L6;
    }

    private location L6() {
        if(Duration.between(clockX, Instant.now()).toMillis() <= 100) {
            System.out.println(OUTPUT_SOUND_ON);
            sound = true;
            return location.L7;
        }
        return location.Done;
    }

    private location L7() {
        try {
            if(Duration.between(clockX, Instant.now()).toMillis() <= 3000){
                    if(inputStream.available() != 0 && reader.readLine().equals(INPUT_UNLOCK)) {
                        clockX = Instant.now();
                        return location.L12;
                    } else {
                        return location.L7;
                    }
            } else if(Duration.between(clockX, Instant.now()).toMillis() > 3000){
                System.out.println(OUTPUT_SOUND_OFF);
                sound = false;
                return location.L8;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Error happend
        return location.Done;
    }

    private location L8(){
        try {
            if(Duration.between(clockX, Instant.now()).toMillis() <= 30000){
                if(inputStream.available() != 0 && reader.readLine().equals(INPUT_UNLOCK)) {
                    clockX = Instant.now();
                    return location.L12;
                } else {
                    Thread.sleep(25);
                    return location.L8;
                }
            } else if(Duration.between (clockX, Instant.now()).toMillis() > 30000){
                System.out.println(OUTPUT_FLASH_OFF);
                return location.L9;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        //Error happend
        return location.Done;
    }

    private location L9() {
        try {
            if (inputStream.available() != 0) {
                String input = reader.readLine();
                if(input.equals(INPUT_CLOSE)) {
                    clockX = Instant.now();
                    return location.L10;
                } else if(input.equals(INPUT_UNLOCK)) {
                    return location.L0;
                }
            }
            Thread.sleep(25);
            return location.L9;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        //Error happend
        return location.Done;
    }

    private location L10(){
        System.out.println(OUTPUT_ARMED_ON);
        return location.L14;
    }

    private location L11(){
        if(Duration.between(clockX, Instant.now()).toMillis() <= 100){
            System.out.println(OUTPUT_ARMED_OFF);
            return location.L1;
        }
        return location.Done;
    }

    private location L12(){
        if(sound) {
            System.out.println(OUTPUT_SOUND_OFF);
            sound = false;
            return location.L12;
        } else if(!sound) {
            System.out.println(OUTPUT_FLASH_OFF);
            return location.L0;
        } else {
            return location.Done;
        }
    }

    private location L14(){
        try {
            String input = reader.readLine();

            if(input.equals(INPUT_UNLOCK)) {
                clockX = Instant.now();
                return location.L11;
            } else if(input.equals(INPUT_OPEN)) {
                clockX = Instant.now();
                return location.L4;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return location.Done;
    }
}
