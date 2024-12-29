package com.j9nos;

public final class Main {
    private Main() {
    }

    public static void main(final String[] args) {
        new RAMUsageControlledBlueLightFilter(new BlueLightController())
                .activate();
    }
}
