package com.j9nos;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;

public final class RAM {
    private RAM() {
    }

    public static int usage() {
        final OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        final long total = os.getTotalMemorySize();
        final long free = os.getFreeMemorySize();
        final long used = total - free;
        return (int) Math.round((double) used / total * 100);
    }

}
