package com.j9nos;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RAMUsageControlledBlueLightFilter {
    private static final long POLLING_RATE = Duration.ofSeconds(2).toMillis();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private final BlueLightController blueLightController;
    private final int savedPercentage;

    public RAMUsageControlledBlueLightFilter(final BlueLightController blueLightController) {
        this.blueLightController = blueLightController;
        savedPercentage = this.blueLightController.readPercentage();
    }

    public void activate() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                EXECUTOR.shutdownNow();
                System.out.println("Bye!");
                blueLightController.updatePercentage(savedPercentage);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }));


        EXECUTOR.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    final int ramUsage = RAM.usage();
                    System.out.println("Current RAM Usage: " + ramUsage + "%");
                    blueLightController.updatePercentage(ramUsage);

                    Thread.sleep(POLLING_RATE);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

}