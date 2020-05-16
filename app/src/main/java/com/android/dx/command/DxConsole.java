package com.android.dx.command;

import java.io.PrintStream;

public class DxConsole {
    public static PrintStream out;
    public static PrintStream err;

    static {
        out = System.out;
        err = System.err;
    }
}
