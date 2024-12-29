package com.j9nos;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class BlueLightController {
    private static final String OS_NAME_PROPERTY_KEY = "os.name";
    private static final Set<String> SUPPORTED_OPERATING_SYSTEMS = new HashSet<>(Set.of("Windows 10"));
    private static final WinReg.HKEY REGISTRY_ROOT = WinReg.HKEY_CURRENT_USER;
    private static final String REGISTRY_KEY_TEMPLATE =
            "Software\\" +
                    "Microsoft\\" +
                    "Windows\\" +
                    "CurrentVersion\\" +
                    "CloudStore\\" +
                    "Store\\" +
                    "DefaultAccount\\" +
                    "Current\\" +
                    "default$windows.data.bluelightreduction.%s\\" +
                    "windows.data.bluelightreduction.%s";
    private static final String STATE_KEY = initKey("bluelightreductionstate");
    private static final String SETTINGS_KEY = initKey("settings");
    private static final String DATA = "Data";


    private static final int STRENGTH_MIN = 1200;
    private static final int STRENGTH_MAX = 6500;
    private static final int STRENGTH_DIFFERENCE = STRENGTH_MIN - STRENGTH_MAX;

    private static String initKey(final String key) {
        return String.format(REGISTRY_KEY_TEMPLATE, key, key);
    }

    public BlueLightController() {
        final String operatingSystem = System.getProperty(OS_NAME_PROPERTY_KEY);
        if (null == operatingSystem || operatingSystem.isBlank()) {
            throw new IllegalArgumentException("Could not detect operating system.");
        }
        if (!SUPPORTED_OPERATING_SYSTEMS.contains(operatingSystem)) {
            throw new UnsupportedOperationException(operatingSystem + " is not supported");
        }
        if (!Advapi32Util.registryKeyExists(REGISTRY_ROOT, STATE_KEY)
                || !Advapi32Util.registryKeyExists(REGISTRY_ROOT, SETTINGS_KEY)) {
            throw new UnsupportedOperationException("Could not find registry key");
        }

        try {
            final byte[] state = state();
            final byte[] settings = settings();
            if (0 == state.length || 0 == settings.length) {
                throw new UnsupportedOperationException("Could not find values");
            }
        } catch (final Win32Exception e) {
            throw new UnsupportedOperationException("Could not find value");
        }
        System.out.println(getClass().getSimpleName() + " is successfully instantiated");
    }

    private byte[] read(final String key) {
        return Advapi32Util.registryGetBinaryValue(REGISTRY_ROOT, key, DATA);
    }

    private byte[] state() {
        return read(STATE_KEY);
    }

    private byte[] settings() {
        return read(SETTINGS_KEY);
    }

    private void updateState(final byte[] newState) {
        final long time = Instant.now().getEpochSecond();
        newState[10] = (byte) ((time & 0x7F) | 0x80);
        newState[11] = (byte) (((time >> 7) & 0x7F) | 0x80);
        newState[12] = (byte) (((time >> 14) & 0x7F) | 0x80);
        newState[13] = (byte) (((time >> 21) & 0x7F) | 0x80);
        newState[14] = (byte) (time >> 28);
        Advapi32Util.registrySetBinaryValue(REGISTRY_ROOT, STATE_KEY, DATA, newState);
    }


    public void turnOn() {
        final byte[] state = state();
        if (0x15 == state[18]) {
            return;
        }
        final byte[] newState = new byte[state.length + 2];
        System.arraycopy(state, 0, newState, 0, 22);
        System.arraycopy(state, 23, newState, 25, 41 - 23);
        newState[18] = 0x15;
        newState[23] = 0x10;
        newState[24] = 0x00;

        updateState(newState);
    }

    public void turnOff() {
        final byte[] state = state();
        if (0x13 == state[18]) {
            return;
        }
        final byte[] newState = new byte[state.length - 2];
        System.arraycopy(state, 0, newState, 0, 22);
        System.arraycopy(state, 25, newState, 23, 43 - 25);
        newState[18] = 0x13;

        updateState(newState);
    }

    public int readPercentage() {
        final byte[] settings = settings();
        final int[] bits = new int[]{
                ((settings[35] - 0x80) / 2) & 0x3F,
                ((settings[36] & 0xFF) << 6) - STRENGTH_MAX
        };
        final int combined = bits[0] | bits[1];
        final double percentage = ((double) combined / STRENGTH_DIFFERENCE) / 0.01;
        return (int) percentage;
    }


}
