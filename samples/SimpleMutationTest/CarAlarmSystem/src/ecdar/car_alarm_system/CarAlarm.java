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

    Instant clockX;
    boolean sound;
    BufferedReader reader;
    InputStream inputStream;


    CarAlarm(){
        inputStream = System.in;
        reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    void start(){
        clockX = Instant.now();
        L0();
    }

    private void L0() {
        try {
            String input = reader.readLine();

            if(input.equals(INPUT_CLOSE)) {
                L1();
            } else if(input.equals(INPUT_LOCK)) {
                L2();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void L1(){
        try {
            String input = reader.readLine();

            if(input.equals(INPUT_OPEN)) {
                L0();
            } else if(input.equals(INPUT_LOCK)) {
                clockX = Instant.now();
                L3();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void L2() {
        try {
            String input = reader.readLine();

            if(input.equals(INPUT_UNLOCK)) {
                L0();
            } else if(input.equals(INPUT_CLOSE)) {
                clockX = Instant.now();
                L3();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void L3() {
        try {
            if (Duration.between(clockX, Instant.now()).toMillis() <= 1900) {
                if (System.in.available() != 0) {
                    final String input = reader.readLine();

                    if(input.equals(INPUT_UNLOCK)) {
                        L1();
                    } else if(input.equals(INPUT_OPEN)) {
                        L2();
                    }
                } else {
                    Thread.sleep(100);
                    L3();
                }
            } else if(Duration.between(clockX, Instant.now()).toMillis() <= 2100){
                System.out.println(OUTPUT_ARMED_ON);
                L14();
            } else {

            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void L4(){
        //Todo
    }

    private void L5(){
        //Todo
    }

    private void L6(){
        //Todo
    }

    private void L7(){
        //Todo
    }

    private void L8(){
        //Todo
    }

    private void L9(){
        //Todo
    }

    private void L10(){
        //Todo
    }

    private void L11(){
        //Todo
    }

    private void L12(){
        //Todo
    }

    private void L13(){
        //Todo
    }

    private void L14(){
        //Todo
    }
}
