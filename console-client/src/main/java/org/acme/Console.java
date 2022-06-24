package org.acme;

public class Console {

    public static final String RESET = "\033[0m";
    public static final String BLACK = "\033[0;30m";
    public static final String RED = "\033[0;31m";
    public static final String GREEN = "\033[0;32m";
    public static final String YELLOW = "\033[0;33m";
    public static final String BLUE = "\033[0;34m";
    public static final String PURPLE = "\033[0;35m";
    public static final String CYAN = "\033[0;36m";
    public static final String WHITE = "\033[0;37m";

    static void green(String format, Object... arguments) {
        println(GREEN, format, arguments);
    }

    static void red(String format, Object... arguments) {
        println(RED, format, arguments);
    }

    static void yellow(String format, Object... arguments) {
        println(YELLOW, format, arguments);
    }

    static void white(String format, Object... arguments) {
        println(WHITE, format, arguments);
    }

    static void cyan(String format, Object... arguments) {
        println(CYAN, format, arguments);
    }

    private static void println(String color, String format, Object... arguments) {
        System.out.println(color + String.format(format, arguments) + RESET);
    }

    static void println(String format, Object... arguments) {
        System.out.println(String.format(format, arguments));
    }

}
