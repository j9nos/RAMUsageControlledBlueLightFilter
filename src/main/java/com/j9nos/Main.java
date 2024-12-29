package com.j9nos;


public class Main {
    public static void main(String[] args) throws InterruptedException {

        final BlueLightController blueLightController = new BlueLightController();
        blueLightController.turnOn();
        Thread.sleep(5_000);
        blueLightController.turnOff();

    }
}