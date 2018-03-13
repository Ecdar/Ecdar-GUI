package ecdar.car_alarm_system;

import java.io.IOException;
import java.time.Instant;

public class Main {

    public static void main(String[] args) {
        CarAlarm alarm = new CarAlarm();
        try {
            alarm.start();
        } catch (Exception e) {
            System.out.println("Debug: Exception " + e);
        }
        System.out.println("Debug: Done");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
