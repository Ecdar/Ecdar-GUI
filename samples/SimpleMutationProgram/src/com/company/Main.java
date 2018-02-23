package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;

public class Main {
    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        Instant x;
        String input;

        System.out.println("a");
        x = Instant.now();
        try {
            input = reader.readLine();
            if (input.equals("c") && Duration.between(x, Instant.now()).toMillis() <= 5000) {
                while (Duration.between(x, Instant.now()).toMillis() <= 4000) {
                    Thread.sleep(1000);
                }
                if (Duration.between(x, Instant.now()).toMillis() <= 5000) {
                    System.out.println("a");
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}