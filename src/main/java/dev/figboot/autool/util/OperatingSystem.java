package dev.figboot.autool.util;

import lombok.Getter;

import java.util.Locale;

public enum OperatingSystem {
    WINDOWS("win"),
    LINUX("linux", "unix"),
    OSX("osx", "mac"),
    SOLARIS("sunos"),
    UNKNOWN();

    private final String[] searchStr;

    OperatingSystem(String... search) {
        this.searchStr = search;
    }

    @Getter private static OperatingSystem OS;

    private static void initOS() {
        if (OS != null) throw new IllegalStateException("OS already initialized");

        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        for (OperatingSystem val : values()) {
            for (String search : val.searchStr) {
                if (osName.contains(search)) {
                    OS = val;
                    return;
                }
            }
        }

        OS = UNKNOWN;
    }

    static {
        initOS();
    }
}
