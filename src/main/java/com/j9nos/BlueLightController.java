package com.j9nos;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public final class BlueLightController {
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
            final byte[] state = readRegistry(STATE_KEY);
            final byte[] settings = readRegistry(SETTINGS_KEY);
            if (0 == state.length || 0 == settings.length) {
                throw new UnsupportedOperationException("Could not find values");
            }
        } catch (final Win32Exception e) {
            throw new UnsupportedOperationException("Could not find value");
        }
    }

    private byte[] readRegistry(final String key) {
        return Advapi32Util.registryGetBinaryValue(REGISTRY_ROOT, key, DATA);
    }

    private void updateRegistry(final String key, final byte[] newData) {
        final long time = Instant.now().getEpochSecond();
        newData[10] = (byte) ((time & 0x7F) | 0x80);
        newData[11] = (byte) (((time >> 7) & 0x7F) | 0x80);
        newData[12] = (byte) (((time >> 14) & 0x7F) | 0x80);
        newData[13] = (byte) (((time >> 21) & 0x7F) | 0x80);
        newData[14] = (byte) (time >> 28);
        Advapi32Util.registrySetBinaryValue(REGISTRY_ROOT, key, DATA, newData);
    }


    public void turnOn() {
        final byte[] state = readRegistry(STATE_KEY);
        if (0x15 == state[18]) {
            return;
        }
        final byte[] newState = new byte[state.length + 2];
        System.arraycopy(state, 0, newState, 0, 22);
        System.arraycopy(state, 23, newState, 25, 18);
        newState[18] = 0x15;
        newState[23] = 0x10;
        newState[24] = 0x00;

        updateRegistry(STATE_KEY, newState);
    }

    public void turnOff() {
        final byte[] state = readRegistry(STATE_KEY);
        if (0x13 == state[18]) {
            return;
        }
        final byte[] newState = new byte[state.length - 2];
        System.arraycopy(state, 0, newState, 0, 22);
        System.arraycopy(state, 25, newState, 23, 18);
        newState[18] = 0x13;

        updateRegistry(STATE_KEY, newState);
    }

    public void updatePercentage(final int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("0-100");
        }

        turnOn();
        final byte[] settings = readRegistry(SETTINGS_KEY);
        final byte[] newSettings = new byte[settings.length];
        System.arraycopy(settings, 0, newSettings, 0, settings.length);

        final int strengthValue = (int) (STRENGTH_MAX + (STRENGTH_DIFFERENCE * (percentage * 0.01)));
        newSettings[35] = ((byte) (((strengthValue & 0x3F) * 2) + 0x80));
        newSettings[36] = (byte) (strengthValue >> 6);
        updateRegistry(SETTINGS_KEY, newSettings);
    }

    public int readPercentage() {
        final byte[] settings = readRegistry(SETTINGS_KEY);
        final int combined = ((settings[35] - 0x80) / 2) & 0x3F | ((settings[36] & 0xFF) << 6) - STRENGTH_MAX;
        final double percentage = ((double) combined / STRENGTH_DIFFERENCE) * 100;
        return (int) percentage;
    }

}
