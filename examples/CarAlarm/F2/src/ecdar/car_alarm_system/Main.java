package ecdar.car_alarm_system;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        CarAlarm alarm = new CarAlarm();
        alarm.start();
    }
}
