package com.j9nos;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RAMUsageControlledBlueLightFilter {
    private static final long POLLING_RATE = Duration.ofSeconds(2).toMillis();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private final BlueLightFilter blueLightFilter;
    private final int savedPercentage;

    public RAMUsageControlledBlueLightFilter(final BlueLightFilter blueLightController) {
        this.blueLightFilter = blueLightController;
        savedPercentage = this.blueLightFilter.readPercentage();
    }

    public void activate() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                EXECUTOR.shutdownNow();
                System.out.println("Bye!");
                blueLightFilter.updatePercentage(savedPercentage);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }));


        EXECUTOR.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    final int ramUsage = RAM.usage();
                    System.out.println("Current RAM Usage: " + ramUsage + "%");
                    blueLightFilter.updatePercentage(ramUsage);

                    Thread.sleep(POLLING_RATE);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

}